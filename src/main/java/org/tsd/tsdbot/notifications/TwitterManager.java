package org.tsd.tsdbot.notifications;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.NotificationType;
import org.tsd.tsdbot.Stage;
import org.tsd.tsdbot.config.TSDBotConfiguration;
import org.tsd.tsdbot.module.NotifierChannels;
import org.tsd.tsdbot.util.IRCUtil;
import org.tsd.tsdbot.util.RelativeDate;
import twitter4j.*;
import twitter4j.auth.AccessToken;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Singleton
public class TwitterManager extends NotificationManager<TwitterManager.Tweet> {

    private static Logger logger = LoggerFactory.getLogger(TwitterManager.class);

    private static final long USER_ID = 2349834990l;
    private static final long EXCEPTION_COOLDOWN = 1000 * 60 * 2; // 2 minutes
    private static final long COOLDOWN_PERIOD = 1000 * 60 * 60 * 2; // 2 hours

    private Stage stage;
    private Twitter twitter;
//    private TwitterStream stream;
    private HashMap<Long, User> following;
    private HashMap<Long, Long> cooldown; // userId -> timestamp of last tweet
    private List<String> channels;

    @Inject
    public TwitterManager(final Bot bot,
                          final Twitter twitter,
                          TSDBotConfiguration configuration,
                          Stage stage,
                          @NotifierChannels Map notifierChannels) throws IOException {
        super(bot, 5, true);
        try {

            this.bot = bot;
            this.stage = stage;
            this.channels = (List<String>) notifierChannels.get("twitter");

            String CONSUMER_KEY =           configuration.twitter.consumerKey;
            String CONSUMER_KEY_SECRET =    configuration.twitter.consumerKeySecret;
            String ACCESS_TOKEN =           configuration.twitter.accessToken;
            String ACCESS_TOKEN_SECRET =    configuration.twitter.accessTokenSecret;

            this.twitter = twitter;
            this.twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_KEY_SECRET);
            this.twitter.setOAuthAccessToken(new AccessToken(ACCESS_TOKEN, ACCESS_TOKEN_SECRET));

            logger.info("Twitter API initialized successfully");

            this.following = new HashMap<>();
            this.cooldown = new HashMap<>();
            Long[] followingIds = ArrayUtils.toObject(twitter.getFriendsIDs(USER_ID, -1).getIDs());
            for(Long id : followingIds) {
                following.put(id, twitter.showUser(id));
                cooldown.put(id, 0L);
            }

        } catch (TwitterException e) {
            logger.error("Twitter Exception", e);
            bot.incrementBlunderCnt();
        }
    }

    public QueryResult search(String queryString, int limit) throws TwitterException {
        Query q = new Query(queryString);
        if(limit > 0)
            q.setCount(limit);
        return twitter.search(q);
    }

    public void checkRateLimit() throws TwitterException {
        Map<String, RateLimitStatus> rateLimitStatusMap = twitter.getRateLimitStatus();
        for(String key : rateLimitStatusMap.keySet()) {
            System.out.println(key + " limit: " + rateLimitStatusMap.get(key).getLimit());
            System.out.println(key + " remaining: " + rateLimitStatusMap.get(key).getRemaining());
            System.out.println(key + " seconds til reset: " + rateLimitStatusMap.get(key).getSecondsUntilReset());
        }
    }

    public Status postTweet(String text) throws TwitterException {
        if(text.length() > 140) throw new TwitterException("Must be 140 characters or less");
        return twitter.updateStatus(text);
    }

    public Status retweet(Tweet toRetweet) throws TwitterException {
        return twitter.retweetStatus(toRetweet.getStatus().getId());
    }

    public Status postReply(Tweet replyTo, String text) throws TwitterException {

        if(text.startsWith("@")) { // text = @whoever I thought you were dead!
            String[] parts = text.split(" ",2);
            if(parts.length > 1) text = parts[1];
        }

        // text = I thought you were dead!

        text = "@" + replyTo.getStatus().getUser().getScreenName() + " " + text;

        // text = @whoever I thought you were dead!

        if(text.length() > 140) throw new TwitterException("Must be 140 characters or less");

        StatusUpdate reply = new StatusUpdate(text);
        reply.setInReplyToStatusId(replyTo.getStatus().getId());

        return twitter.updateStatus(reply);
    }

    public void follow(String channel, String handle) {
        handle = handle.replace("@","");
        try {
            User followed = twitter.createFriendship(handle);
            if(followed != null) {
                following.put(followed.getId(), followed);
                cooldown.put(followed.getId(), 0L);
                refreshFollowersFilter();
                bot.sendMessage(channel, "Now following @" + followed.getScreenName());
            }
        } catch (TwitterException e) {
            bot.sendMessage(channel, "I could not follow @" + handle + ". Maybe they don't exist?");
            bot.incrementBlunderCnt();
        }
    }

    public void unfollow(String channel, String handle) {
        handle = handle.replace("@","");
        try {
            User unfollowed = twitter.destroyFriendship(handle);
            if(unfollowed != null) {
                following.remove(unfollowed.getId());
                cooldown.remove(unfollowed.getId());
                refreshFollowersFilter();
                bot.sendMessage(channel, "No longer following @" + unfollowed.getScreenName());
            }
        } catch (TwitterException e) {
            bot.sendMessage(channel, "I could not unfollow @" + handle + ". Maybe they don't exist?");
            bot.incrementBlunderCnt();
        }
    }

    public void unleash(String channel, String handle) {
        handle = handle.replace("@","");
        for(User followed : following.values()) {
            if(followed.getScreenName().equalsIgnoreCase(handle)) {
                cooldown.remove(followed.getId());
                bot.sendMessage(channel, "@" + handle + " has been UNLEASHED!");
                return;
            }
        }
        bot.sendMessage(channel, "I could not unleash @" + handle + " because I'm not following xir");
        bot.incrementBlunderCnt();
    }

    public void throttle(String channel, String handle) {
        handle = handle.replace("@","");
        for(User followed : following.values()) {
            if(followed.getScreenName().equalsIgnoreCase(handle)) {
                if(cooldown.containsKey(followed.getId())) {
                    bot.sendMessage(channel, "@" + handle + " is already being throttled");
                } else {
                    cooldown.put(followed.getId(), 0L);
                    bot.sendMessage(channel, "@" + handle + " has been restrained");
                }
                return;
            }
        }
        bot.sendMessage(channel, "I could not throttle @" + handle + " because I'm not following xir");
        bot.incrementBlunderCnt();
    }

    public void delete(String channel, long id) {
        try {
            Status deleted = twitter.destroyStatus(id);
            if(deleted != null) {
                bot.sendMessage(channel, "Successfully deleted status");
            } else {
                bot.sendMessage(channel, "Couldn't delete status. Maybe it doesn't exist?");
            }
        } catch (TwitterException e) {
            String msg = "Error deleting status " + id;
            logger.error(msg, e);
            bot.sendMessage(channel, msg);
        }
    }

    public LinkedList<String> getFollowing() throws TwitterException {
        LinkedList<String> ret = new LinkedList<>();
        for(Long id : following.keySet()) {
            User f = following.get(id);
            ret.add(f.getName() + " (@" + f.getScreenName() + ")");
        }
        return ret;
    }

    private void refreshFollowersFilter() throws TwitterException {
//        if(stage.equals(Stage.production))
//            stream.filter(new FilterQuery(ArrayUtils.toPrimitive(following.keySet().toArray(new Long[]{}))));
    }

    @Override
    public LinkedList<Tweet> sweep() {
        return new LinkedList<>();
    }

    @Override
    public NotificationType getNotificationType() {
        return NotificationType.TWITTER;
    }

    public class Tweet extends NotificationEntity {

        private Status status;

        public Tweet(Status status) {
            this.status = status;
        }

        public Status getStatus() {
            return status;
        }

        public String getTrimmedId() {
            String asString = String.valueOf(status.getId());
            return asString.substring(asString.length()-4); // ...4321
        }

        @Override
        public String getInline() {
            return IRCUtil.trimToSingleMsg("[Twitter] " + "[" + status.getUser().getName() + " @" + status.getUser().getScreenName() + "] " + status.getText() + " (" + RelativeDate.getRelativeDate(status.getCreatedAt()) + ") id=" + getTrimmedId()) ;
        }

        @Override
        public String getPreview() {
            setOpened(true);
            return IRCUtil.trimToSingleMsg("[" + status.getUser().getName() + " @" + status.getUser().getScreenName() + "] " + status.getText() + " (" + RelativeDate.getRelativeDate(status.getCreatedAt()) + ") id=" + getTrimmedId()) ;
        }

        @Override
        public String[] getFullText() {
            setOpened(true);
            return IRCUtil.splitLongString(status.getText() + " (" + RelativeDate.getRelativeDate(status.getCreatedAt()) + ")") ;
        }

        @Override
        public String getKey() {
            return "" + status.getId();
        }
    }

    class DelayedImpl implements Delayed {

        private long finishTime;

        public DelayedImpl(long delay) {
            this.finishTime = System.currentTimeMillis() + delay;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(finishTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
        }
    }
}
