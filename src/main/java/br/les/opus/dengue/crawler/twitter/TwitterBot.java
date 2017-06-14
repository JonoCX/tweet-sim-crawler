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
public class TwitterBot extends TwitterWorker {
    // We've noticed that you've posted something important. Please go here for more information: http://bit.ly/2ska3pz
    private static final String REPLY = "Percebemos que publicou algo importante. " +
            "Por favor, acesse aqui para obter mais informações: http://bit.ly/2ska3pz";


    @Autowired
    private TweetBotRepository botRepository;

    @Override
    protected void execute() throws WorkerException {
        ConfigurationBuilder builder = super.getConfigurationBuilder();
        TwitterFactory f = new TwitterFactory(builder.build());
        Twitter twitter = f.getInstance();

        StatusUpdate statusUpdate;
        TweetBot tweetBot;
        List<TweetBot> tweets = botRepository.findAllNotRepliedTo();

        for (TweetBot tb : tweets) {
            if (tb.getReplied() == Boolean.FALSE) {
                String replyString = "@" + tb.getScreenName() + " " + REPLY;
                statusUpdate = new StatusUpdate(replyString);
                statusUpdate.setInReplyToStatusId(tb.getTweetId());

                logger.info("Replying to tweet: https://twitter.com/" + tb.getScreenName() + "/status/" + tb.getTweetId());
                try {
                    twitter.updateStatus(statusUpdate);

                    tb.setTimeReplied(new Date());
                    tb.setReplied(true);
                    botRepository.save(tb);
                } catch (TwitterException te) {
                    logger.error("Could not reply to status: " + tb.getTweetId());
                }
            } else {
                logger.info("Tweet " + tb.getTweetId() + " already been processed.");
            }
        }
    }
}

