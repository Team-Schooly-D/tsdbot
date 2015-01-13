package org.tsd.tsdbot;

import com.google.inject.servlet.ServletModule;
import org.tsd.tsdbot.servlets.StatusServlet;
import org.tsd.tsdbot.servlets.hustle.HustleChartServlet;
import org.tsd.tsdbot.servlets.hustle.HustleServlet;
import org.tsd.tsdbot.servlets.tsdtv.TSDTVCatalogServlet;
import org.tsd.tsdbot.servlets.tsdtv.TSDTVPlayServlet;

/**
 * Created by Joe on 1/11/2015.
 */
public class TSDBotServletModule extends ServletModule {
    @Override
    protected void configureServlets() {

        bind(HustleServlet.class);
        serve("/hustle").with(HustleServlet.class);
        bind(HustleChartServlet.class);
        serve("/hustle/chart").with(HustleChartServlet.class);

        bind(TSDTVCatalogServlet.class);
        serve("/tsdtv/catalog").with(TSDTVCatalogServlet.class);
        bind(TSDTVPlayServlet.class);
        serve("/tsdtv/play").with(TSDTVPlayServlet.class);

        bind(StatusServlet.class);
        serve("/status").with(StatusServlet.class);

    }
}
