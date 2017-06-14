package br.les.opus.dengue.crawler.twitter;

import br.les.opus.dengue.crawler.WorkerException;
import br.les.opus.twitter.domain.Tweet;
import br.les.opus.twitter.domain.TweetBot;
import br.les.opus.twitter.repositories.TweetBotRepository;
import br.les.opus.twitter.repositories.TweetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jonathan Carlton
 */
@Component
@Transactional
public class TwitterBot extends TwitterWorker
{
    // We've noticed that you've posted something important. Please go here for more information: http://bit.ly/2ska3pz
    private static final String REPLY = "Percebemos que publicou algo importante. " +
            "Por favor, acesse aqui para obter mais informações: http://bit.ly/2ska3pz";

    // Prevent the same tweet being replied to twice.
    private Map<Tweet, Boolean> ids;

    @Autowired
    private TweetBotRepository botRepository;

    public TwitterBot() {
        ids = new HashMap<>();
    }

    public void addToList(List<Tweet> list) {
        for (Tweet i : list) {
            if (!(ids.containsKey(i))) {
                ids.put(i, Boolean.FALSE);
            }
        }
    }

    public void addToList(Tweet id) {
        if (!(ids.containsKey(id))) {
            ids.put(id, Boolean.FALSE);
        }
    }


    private Twitter getTwitterApi() {
        ConfigurationBuilder builder = getConfigurationBuilder();
        TwitterFactory f = new TwitterFactory(builder.build());
        return f.getInstance();
    }

    @Override
    protected void execute() throws WorkerException {
        Twitter twitter = this.getTwitterApi();

        StatusUpdate statusUpdate;
        TweetBot tweetBot;
        for (Map.Entry<Tweet, Boolean> m : ids.entrySet()) {
            if (m.getValue() == Boolean.FALSE) {
                Tweet tweet = m.getKey();
                String replyString = "@" + tweet.getUser().getScreenName() + " " + REPLY;
                statusUpdate = new StatusUpdate(replyString);
                statusUpdate.setInReplyToStatusId(tweet.getId());

                logger.info("Replying to tweet: " + tweet.getId());

                try {
                    twitter.updateStatus(statusUpdate);

                    tweetBot = new TweetBot();
                    tweetBot.setTweetId(tweet.getId());
                    tweetBot.setScreenName(tweet.getUser().getScreenName());
                    tweetBot.setTimePosted(tweet.getCreatedAt());
                    tweetBot.setTimeReplied(new Date());
                    botRepository.save(tweetBot);

                    ids.put(tweet, Boolean.TRUE); // mark that it's been replied too
                } catch (TwitterException te) {
                    logger.error("Could not reply to status: " + tweet.getId());
                }
            } else {
                logger.info("Tweet " + m.getKey().getId() + " already been processed.");
            }
        }
    }
}
