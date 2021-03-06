package br.les.opus.dengue.crawler.twitter;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.les.opus.dengue.crawler.AbstractWorker;
import br.les.opus.dengue.crawler.WorkerException;
import br.les.opus.twitter.domain.Tweet;
import br.les.opus.twitter.domain.TweetClassification;
import br.les.opus.twitter.domain.TweetsMetadata;
import br.les.opus.twitter.domain.TwitterUser;
import br.les.opus.twitter.domain.TweetBot;
import br.les.opus.twitter.repositories.TweetClassificationRepository;
import br.les.opus.twitter.repositories.TweetRepository;
import br.les.opus.twitter.repositories.TweetsMetadataRepository;
import br.les.opus.twitter.repositories.TweetBotRepository;
import br.les.opus.twitter.repositories.TwitterUserRepository;
import uk.ac.ncl.cc.classifier.Category;
import uk.ac.ncl.cc.classifier.Classifier;

@Component
@Transactional
public class TwitterClassifier extends AbstractWorker {

	@Autowired
	private TweetRepository tweetRepository;

	@Autowired
	private TweetClassificationRepository tweetClassificationDao;

	@Autowired
	private TweetBotRepository botRepo;

	@Autowired
	private TweetsMetadataRepository metaDao;

	@Autowired
	private TwitterUserRepository userDao;

	@Autowired
	private SessionFactory sessionFactory;

	private Classifier classifier;

	@Autowired
	private TwitterBot bot;

	private Logger logger;

	public TwitterClassifier() {
		this.logger = Logger.getLogger(TwitterClassifier.class);
	}

	public void classify(Tweet tweet) throws Exception {
		Category category = classifier.classify(tweet.getText());
		TweetClassification classification = tweetClassificationDao.findByKey(category.toString());
		tweet.setClassification(classification);
	}

	private void commit() {
		Session session = sessionFactory.getCurrentSession();
		session.getTransaction().commit();
		sessionFactory.getCurrentSession().beginTransaction();
	}

	public void updateUserMetadata(Tweet tweet) {
		TweetsMetadata metadata = metaDao.findOne(tweet.getUser(), tweet.getClassification());
		if (metadata != null) {
			metadata.incrementCount();
		} else {
			metadata = new TweetsMetadata(tweet);
		}
		metaDao.save(metadata);
	}


	public void setOutdatedFollowers(Tweet tweet) {
		TwitterUser user = tweet.getUser();
		user.setOutdatedFollowers(true);
		userDao.save(user);
	}

	public List<Tweet> startClassification() throws WorkerException {
		List<Tweet> ids = new ArrayList<>();
		try {
			List<Tweet> tweets = tweetRepository.findAllUnclassifiedByLanguage("pt");
			logger.info("Iniciando classificação de todos os não classificados.");
			logger.info("Tweets to classify: " + tweets.size());

			int count = 0;
			for (Tweet tweet : tweets) {
				this.classify(tweet);
				tweetRepository.save(tweet);
				if (tweet.getClassification().isUsedInTwitterRank()) {
					this.updateUserMetadata(tweet);
					this.setOutdatedFollowers(tweet);
				}
				if(tweet.getClassification().getId() == 3){
					TweetBot tb = new TweetBot();
					tb.setTweetId(tweet.getId());
					tb.setScreenName(tweet.getUser().getScreenName());
					tb.setTimePosted(tweet.getCreatedAt());
					tb.setReplied(false);
					botRepo.save(tb);
				}
				count++;
				if (count % 100 == 0) {
					this.commit();
					logger.info(count + " tweets classified");
				}
			}
			logger.info("tweets classified");
			this.commit();
		} catch (Exception e) {
			throw new WorkerException(e.getMessage(), e);
		}
		return ids;
	}

	@Override
	@Transactional(value = TxType.REQUIRES_NEW)
	protected void execute() {

		try {
			logger.info("Classifier loading...");
			this.classifier = Classifier.getInstance();
			this.bot = new TwitterBot();
			logger.info("loaded!");

			//for(int i = 0; i < 50; i++) {
				this.startClassification();
				//System.out.println("sleeping");
				//Thread.sleep(100000);
			//}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
