package org.tsd.tsdbot.functions;

import org.jibble.pircbot.User;
import org.tsd.tsdbot.TSDBot;

import java.util.Date;

/**
 * Created by Joe on 5/24/14.
 */
public abstract class MainFunction {

    private Long cooldownMillis = null;
    private Date lastUsed = null;

    protected MainFunction(int cooldownMinutes) {
        this.cooldownMillis = (long)cooldownMinutes * 60 * 1000;
    }

    protected MainFunction() {}

    public void engage(String channel, String sender, String ident, String text) {
        long timeRemaining = getRemainingCooldown(); // millis
        if(timeRemaining <= 0 || TSDBot.getInstance().getUserFromNick(channel, sender).hasPriv(User.Priv.OP)) {
            run(channel, sender, ident, text);
            lastUsed = new Date();
        } else {
            int minutesLeft = (int)Math.ceil( ((double)timeRemaining) / ((double)(1000 * 60)) ); // necessary?
            TSDBot.getInstance().sendMessage(channel, "That function will be available in " + minutesLeft + " minute(s)");
        }
    }

    protected abstract void run(String channel, String sender, String ident, String text);

    private long getRemainingCooldown() {
        if(cooldownMillis == null || lastUsed == null) return -1; // no cooldown or hasn't been run yet
        return lastUsed.getTime() + cooldownMillis - System.currentTimeMillis();
    }
}
