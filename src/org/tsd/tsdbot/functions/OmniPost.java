package org.tsd.tsdbot.functions;

import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.notifications.NotificationEntity;
import org.tsd.tsdbot.notifications.NotificationManager;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Joe on 5/24/14.
 */
public class OmniPost implements MainFunction {
    
    @Override
    public void run(String channel, String sender, String text) {
        
        TSDBot bot = TSDBot.getInstance();
        String[] cmdParts = text.split("\\s+");
        List<TSDBot.Command> matchingCommands = TSDBot.Command.fromString(cmdParts[0]);
        if(matchingCommands.size() != 1) return;
        TSDBot.Command command = matchingCommands.get(0);
        
        TSDBot.NotificationType type = TSDBot.NotificationType.fromCommand(command);
        NotificationManager<NotificationEntity> mgr = bot.getNotificationManagers().get(type);

        if(cmdParts.length == 1) {
            bot.sendMessage(channel,command.getUsage());
        } else if(cmdParts[1].equals("list")) {
            if(mgr.history() == null || mgr.history().isEmpty())
                bot.sendMessage(channel, "No " + type.getDisplayString() + " posts in recent history");
            for(NotificationEntity notification : mgr.history()) {
                bot.sendMessage(channel,notification.getInline());
            }
        } else if(cmdParts[1].equals("pv")) {
            if(mgr.history().isEmpty()) {
                bot.sendMessage(channel,"No " + type.getDisplayString() +" posts in recent history");
            } else if(cmdParts.length == 2) {
                NotificationEntity mostRecent = mgr.history().getFirst();
                if(mostRecent.isOpened()) bot.sendMessage(channel,"Post " + mostRecent.getKey() + " has already been opened");
                else bot.sendMessage(channel,mostRecent.getPreview());
            } else {
                String postKey = cmdParts[2].trim();
                LinkedList<NotificationEntity> ret = mgr.getNotificationByTail(postKey);
                if(ret.size() == 0) bot.sendMessage(channel,"Could not find " + type.getDisplayString() + " post with ID " + postKey + " in recent history");
                else if(ret.size() > 1) {
                    String returnString = "Found multiple matching " + type.getDisplayString() + " posts in recent history:";
                    for(NotificationEntity not : ret) returnString += (" " + not.getKey());
                    returnString += ". Help me out here";
                    bot.sendMessage(channel,returnString);
                }
                else bot.sendMessage(channel,ret.get(0).getPreview());
            }
        } else {
            bot.sendMessage(channel,command.getUsage());
        }
    }
}
