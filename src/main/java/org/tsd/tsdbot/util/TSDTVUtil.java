package org.tsd.tsdbot.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Joe on 1/12/2015.
 */
public class TSDTVUtil {

    public static int getEpisodeNumberFromFilename(String fileName) throws Exception {
        Pattern episodeNumberPattern = Pattern.compile("^(\\d+).*",Pattern.DOTALL);
        Matcher epNumMatcher = episodeNumberPattern.matcher(fileName);
        while(epNumMatcher.find()) {
            return Integer.parseInt(epNumMatcher.group(1));
        }
        throw new Exception("Could not parse episode number from String " + fileName);
    }
}