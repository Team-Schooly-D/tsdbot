package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.ThreadType;
import org.tsd.tsdbot.runnable.DorjThread;
import org.tsd.tsdbot.runnable.InjectableIRCThreadFactory;
import org.tsd.tsdbot.runnable.StrawPoll;
import org.tsd.tsdbot.runnable.ThreadManager;

/**
 * Created by Joe on 5/24/14.
 */
@Singleton
public class Dorj extends MainFunction {

    private static final Logger logger = LoggerFactory.getLogger(Dorj.class);

    private InjectableIRCThreadFactory threadFactory;
    private ThreadManager threadManager;

    @Inject
    public Dorj(TSDBot bot, ThreadManager threadManager, InjectableIRCThreadFactory threadFactory) {
        super(bot);
        this.description = "Dorj: use teamwork to summon the legendary Double Dorj";
        this.usage = "USAGE: .dorj";
        this.threadManager = threadManager;
        this.threadFactory = threadFactory;
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {
        DorjThread existingThread = (DorjThread) threadManager.getIrcThread(ThreadType.DORJ, channel);
        if(existingThread == null) try {
            existingThread = threadFactory.newDorjThread(channel, ident);
            threadManager.addThread(existingThread);
        } catch (Exception e) {
            logger.error("Error building Dorj thread", e);
            bot.sendMessage(channel, "Error building Dorj thread: " + e.getMessage());
        }
    }

    @Override
    public String getRegex() {
        return "^\\.dorj$";
    }
}