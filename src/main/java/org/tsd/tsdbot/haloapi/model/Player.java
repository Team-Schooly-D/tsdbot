package org.tsd.tsdbot.haloapi.model;

public class Player {

    // The player's gamertag.
    String Gamertag;

    // Internal use only. This will always be null.
    Object Xuid;

    public String getGamertag() {
        return Gamertag;
    }

    public Object getXuid() {
        return Xuid;
    }
}
