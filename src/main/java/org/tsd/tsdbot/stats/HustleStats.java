package org.tsd.tsdbot.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.util.ShapeUtilities;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.util.CircularBuffer;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Joe on 1/11/2015.
 */
@Singleton
public class HustleStats implements Stats {

    private static final Logger log = LoggerFactory.getLogger(HustleStats.class);

    private static final int period = 5; // every [period]th message will be sent for hustle analysis
    private static final DecimalFormat decimalFormat = new DecimalFormat("##0.00");
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss a z");

    static {
        timeFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
    }

    private TSDBot bot;

    private HttpClient httpClient;
    private String apiKey;

    private DataPoint lastDataPoint = null;
    private CircularBuffer<DataPoint> hustleBuffer = new CircularBuffer<>(50);

    private int msgCnt = 0;

    @Inject
    public HustleStats(TSDBot bot, HttpClient httpClient, Properties properties) {
        this.bot = bot;
        this.httpClient = httpClient;
        this.apiKey = properties.getProperty("mashape.apiKey");
    }

    @Override
    public HashMap<String, Object> getReport() {
        HashMap<String, Object> report = new HashMap<>();
        report.put("Current HHR", "<a href=\"/hustle\">" + calculateCurrentHhr() + "</a>");
        return report;
    }

    @Override
    public void processMessage(String channel, String sender, String login, String hostname, String message) {
        if(++msgCnt < period)
            return;
        else msgCnt = 0;

        log.info("Sending latest message for sentiment analysis...");

        HttpPost post = null;
        try {
            post = new HttpPost("https://community-sentiment.p.mashape.com/text/");
            post.addHeader("X-Mashape-Key", apiKey);
            post.addHeader("Content-Type", "application/x-www-form-urlencoded");

            LinkedList<NameValuePair> params = new LinkedList<>();
            params.add(new BasicNameValuePair("txt", message));
            post.setEntity(new UrlEncodedFormEntity(params));
            CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(post);
            String responseString = EntityUtils.toString(response.getEntity());

            JSONObject json = new JSONObject(responseString);
            for(String key : json.keySet()) {
                if(key.equals("result")) {
                    double confidence = Double.parseDouble(json.getJSONObject(key).getString("confidence"));
                    Sentiment sentiment = Sentiment.fromString(json.getJSONObject(key).getString("sentiment"));
                    log.info("Analysis result: {} (Confidence {})", sentiment, confidence);
                    double lastHhr = (lastDataPoint == null) ? 0 : lastDataPoint.newHhr;
                    log.info("Previous HHR: {}", lastHhr);
                    lastDataPoint = new DataPoint(message, sentiment, confidence);
                    hustleBuffer.add(lastDataPoint);
                    lastDataPoint.newHhr = calculateCurrentHhr();
                    lastDataPoint.delta = lastDataPoint.newHhr - lastHhr;
                    log.info("New HHR: {} (delta {})", lastDataPoint.newHhr, lastDataPoint.delta);
                }
            }

            EntityUtils.consumeQuietly(response.getEntity());

        } catch (Exception e) {
            log.error("Error retrieving text sentiment", e);
            bot.sendMessage(channel, "(Error calculating hustle quotient, please check logs)");
        } finally {
            if(post != null)
                post.releaseConnection();
        }
    }

    @Override
    public void processAction(String sender, String login, String hostname, String target, String action) {}

    public JFreeChart generateChart() {
        TreeSet<DataPoint> orderedByImpact = new TreeSet<>(new Comparator<DataPoint>() {
            @Override
            public int compare(DataPoint o1, DataPoint o2) {
                return Double.compare(Math.abs(o1.delta), Math.abs(o2.delta));
            }
        });

        final TimeSeries timeSeries = new TimeSeries("Hustle");
        for (DataPoint dataPoint : hustleBuffer) {
            Second date = new Second(dataPoint.date);
            timeSeries.add(date, dataPoint.newHhr);
            orderedByImpact.add(dataPoint);
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection(timeSeries);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Hustle/Hate ratio",
                "Time",
                "HHR",
                dataset
        );

        XYPlot plot = chart.getXYPlot();

        DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
        domainAxis.setDateFormatOverride(timeFormat);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setNumberFormatOverride(decimalFormat);
        double mid = rangeAxis.getRange().getCentralValue();

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesShape(0, ShapeUtilities.createDiamond(5));
        renderer.setSeriesShapesVisible(0, true);

        int limit = 5;
        int i = 0;
        for(DataPoint dp : orderedByImpact.descendingSet()) {
            if(i++ > limit)
                break;
            log.info("Adding annotation, hhr = {}, delta = {}", dp.newHhr, dp.delta);
            TimeSeriesDataItem importantItem = timeSeries.getDataItem(new Second(dp.date));
            double x = importantItem.getPeriod().getFirstMillisecond();
            double y = importantItem.getValue().doubleValue();
            double r = (y > mid) ? (Math.PI / 2) : (3 * Math.PI / 2);
            String s = trimMessage(dp.text);
            XYPointerAnnotation a = new XYPointerAnnotation(s, x, y, r);
            a.setLabelOffset(10);
            a.setFont(new Font("SansSerif", Font.PLAIN, 12));
            a.setOutlineStroke(new BasicStroke(5));
            plot.addAnnotation(a);
        }

        return chart;
    }

    private double calculateCurrentHhr() {
        double hustle = 1;
        double hate = 1;
        for(DataPoint dataPoint : hustleBuffer) {
            switch (dataPoint.sentiment) {
                case Positive: hustle += dataPoint.getScore(); break;
                case Negative: hate += dataPoint.getScore(); break;
                case Neutral: {
                    hustle += dataPoint.getScore();
                    hate += dataPoint.getScore();
                    break;
                }
            }
        }
        return hustle/hate;
    }

    private String trimMessage(String text) {
        int maxChars = 50;
        String[] words = text.split("\\s+");
        StringBuilder sb = new StringBuilder();
        int i_c = 0;
        for(String word : words) {
            if(i_c > maxChars) {
                sb.append("...");
                return sb.toString();
            } else if(i_c != 0)  {
                sb.append(" ");
                i_c++;
            }
            sb.append(word);
            i_c += word.length();
        }
        return sb.toString();
    }

    enum Sentiment {
        Positive,
        Negative,
        Neutral;

        public static Sentiment fromString(String s) {
            for(Sentiment sentiment : values()) {
                if(sentiment.toString().toLowerCase().equals(s.toLowerCase()))
                    return sentiment;
            }
            return null;
        }
    }

    class DataPoint {

        public String text;
        public Sentiment sentiment;
        public double confidence;
        public Date date;
        public double newHhr;
        public double delta;

        DataPoint(String text, Sentiment sentiment, double confidence) {
            this.text = text;
            this.sentiment = sentiment;
            this.confidence = confidence;
            this.date = new Date();
        }

        public double getScore() {
            return (confidence/100) * text.length();
        }
    }
}