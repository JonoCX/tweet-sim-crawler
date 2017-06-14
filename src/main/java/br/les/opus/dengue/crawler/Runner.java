package br.les.opus.dengue.crawler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.transaction.Transactional;

import br.les.opus.dengue.crawler.twitter.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import br.les.opus.dengue.crawler.instagram.InstagramStarter;
import br.les.opus.dengue.crawler.instagram.MediaUpdateCrawler;
import twitter4j.Twitter;

@Component
@Transactional
public class Runner {
	
	private static final String TWITTER = "twitter";
	
	private static final String INSTAGRAM = "instagram";
	
	private static final String INSTAGRAM_MEDIA_UPDATE = "instagram-media-update";
	
	private static final String CLASSIFIER = "twitter-classifier";
	
	private static final String FOLLOWERS_FETCHER = "twitter-followers-fetcher";
	
	private static final String VALIDATION = "validation";

	private static final String BOT = "bot";
	
	
	@Autowired
	private TwitterFollowersFetcher followersFetcher;
	
	@Autowired
	private TwitterCrawler twitterCrawler;
	
	@Autowired
	private InstagramStarter instagramStarter;
	
	@Autowired
	private MediaUpdateCrawler mediaUpdateCrawler;
	
	@Autowired
	private TwitterClassifier classifier;
	
	@Autowired
	private TweetValidation tweetValidation;

	@Autowired
	private TwitterBot bot;
	
	protected static Logger logger = Logger.getLogger(Runner.class);

	private Map<String, AbstractWorker> getWorkers() {
		Map<String, AbstractWorker> workers = new HashMap<>();
		workers.put(TWITTER, twitterCrawler);
		workers.put(INSTAGRAM, instagramStarter);
		workers.put(INSTAGRAM_MEDIA_UPDATE, mediaUpdateCrawler);
		workers.put(CLASSIFIER, classifier);
		workers.put(FOLLOWERS_FETCHER, followersFetcher);
		workers.put(VALIDATION, tweetValidation);
		workers.put(BOT, bot);
		return workers;
	}
	
	public boolean isValidOption(String option) {
		return getWorkers().keySet().contains(option);
	}
	
	public Set<String> getValidOptions() {
		return getWorkers().keySet();
	}
	
	@Transactional
	public void run(String option) throws WorkerException {
		getWorkers().get(option).start();
	}
	
	
	public static void main(String[] args) {
		try {
			@SuppressWarnings("resource")
			ApplicationContext context = new ClassPathXmlApplicationContext("crawler-app-config.xml");
			Runner runner = (Runner)context.getBean("runner");
			
//			if (args.length < 1) {
//				logger.fatal("You need to specify at least ONE parameter: " + runner.getValidOptions());
//				System.exit(1);
//			}
			
			//String crawler = args[0];
			String crawler = "twitter-classifier";
			if (runner.isValidOption(crawler)) {
				logger.info("Running with option: " + crawler);
				runner.run(crawler);
			}  else {
				logger.fatal("You need to specify one of these parameters: " + runner.getValidOptions());
				System.exit(1);
			}
			if(crawler.equals("twitter-classifier")) {

				runner.getWorkers().get("twitter-classifier").join();
				runner.getWorkers().get("bot").start();
				runner.getWorkers().get("bot").join();

			}
		} catch (Exception e) {
			//
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}
	}
}
