package br.les.opus.dengue.crawler.twitter;

import java.util.Date;
import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import br.les.opus.dengue.crawler.WorkerException;
import br.les.opus.twitter.domain.HashTag;
import br.les.opus.twitter.domain.TwitterUser;
import br.les.opus.twitter.domain.Validation;
import br.les.opus.twitter.domain.ValidationMetadata;
import br.les.opus.twitter.repositories.HashTagRepository;
import br.les.opus.twitter.repositories.TweetsMetadataRepository;
import br.les.opus.twitter.repositories.TwitterUserRepository;
import br.les.opus.twitter.repositories.ValidationMetadataRepository;
import br.les.opus.twitter.repositories.ValidationRepository;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

@Component
@Transactional
public class TweetValidation extends TwitterWorker {

	private Gson gson;

	private Logger logger;

	@Autowired
	private ValidationMetadataRepository VMrepository;

	@Autowired
	private ValidationRepository Vrepository;

	@Autowired
	private TwitterUserRepository Urepository;

	@Autowired
	private TweetsMetadataRepository TMrepository;

	@Autowired
	private HashTagRepository Hrepository;
	
	long DAY_IN_MS = 1000 * 60 * 60 * 24;
	private int monthsBackwardsInDays = 67;
	private int daysBackwards = 1;
	private long threeMonths;
	private long yesterday;
	private long today;
	private boolean badUser;
	private boolean updateUser;

	public TweetValidation() {
		this.gson = new Gson();
		this.logger = Logger.getLogger(TweetValidation.class);
	}

	protected String getKeyWords() {
		List<HashTag> tags = Hrepository.findAllActive();
		String[] tagsArray = new String[tags.size()];
		int i = 0;
		String keys = "";
		for (HashTag hashTag : tags) {
			tagsArray[i++] = hashTag.getText();
			keys += hashTag.getText();
			if (i != tags.size() - 1) {
				keys += " OR ";
			}
		}
		return keys;
	}

	protected void setDates() {
		// set date to however many days in the past
		Date months = new Date(System.currentTimeMillis() - (monthsBackwardsInDays * DAY_IN_MS));
		Date oneDay = new Date(System.currentTimeMillis() - (daysBackwards * DAY_IN_MS));

		// get tweet id from start and end of timeframe
		threeMonths = Vrepository.findMinId(months);
		yesterday = Vrepository.findMinId(oneDay);
		today = Vrepository.findMaxId();
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	protected void fetchAndSaveAll(Twitter twitter, String user, long since, long until) {
		// log users screenname
		logger.info("Getting tweets for user: " + user);

		// set up paging - max per page is 200 set by Twitter
		Paging p = new Paging();
		p.setCount(200);

		// set ids to min and max
		p.setSinceId(since);
		p.setMaxId(until);

		List<Status> tweets = null;
		int i = 1;
		
		// watch out for rate limit
		try {
			if (twitter.getRateLimitStatus().get("/statuses/user_timeline").getRemaining() < 50) {
				logger.info("Adding wait of - "
						+ twitter.getRateLimitStatus().get("/statuses/user_timeline").getSecondsUntilReset()
						+ " seconds");
				Thread.sleep((twitter.getRateLimitStatus().get("/statuses/user_timeline").getSecondsUntilReset() + 10)
						* 1000);
			}

			// get first page
			tweets = twitter.getUserTimeline(user, p);
			saveTweets(tweets);

			// loop pages and save tweets
			while (tweets.size() != 0) {
				p.setPage(i++);
				tweets = twitter.getUserTimeline(user, p);
				saveTweets(tweets);
			}

			// throw exception
		} catch (TwitterException | InterruptedException e) {
			logger.info("Can not get tweets. Private or Non existent user...");
			// TODO handle bad users here.
			logger.info("User removed.");
			badUser = true;
		}
	}

	public void saveTweets(List<Status> result) {

		for (Status status : result) {

			Status currentTweet = status;
			
			String json = gson.toJson(status);
			Validation tweetToSave = gson.fromJson(json, Validation.class);
			

			// save geo location if available
			if (currentTweet.getGeoLocation() != null) {
				tweetToSave.setGeolocation(status.getGeoLocation());
			}
			
        	Vrepository.save(tweetToSave);
		}
		logger.info("Completed page...");
	}
	


	private void saveValidation(Twitter twitter, TwitterUser user) {
		double topicFocus;
		double recall;
		double overallFocus;
		ValidationMetadata vm;
		
		System.out.println("Saving validation for: " + user.getScreenName());
		
		try {
			vm = VMrepository.findOne(user);
		}
		catch(Exception e) {
			vm = new ValidationMetadata();
		}
		
		vm.setUser(user);
		vm.setNewsCount(VMrepository.countNewsTweetsFrom(user));
		vm.setNoiseCount(VMrepository.countNoiseTweetsFrom(user));
		vm.setRelevantCount(VMrepository.countRelevantTweetsFrom(user));
		vm.setTweetsCount(VMrepository.countPublishedTweetsFrom(user));
		vm.setZikaCount(vm.getNewsCount() + vm.getNoiseCount() + vm.getRelevantCount());

		if (vm.getZikaCount() != 0) {
			topicFocus = ((double) vm.getRelevantCount() / (double) vm.getZikaCount());
		} else {
			topicFocus = -1;
		}
		if (vm.getTweetsCount() != 0) {
			overallFocus = ((double) vm.getRelevantCount() / (double) vm.getTweetsCount());
			recall = ((double) vm.getZikaCount() / (double) vm.getTweetsCount());
		} else {
			overallFocus = -1;
			recall = -1;
		}

		//System.out.println(topicFocus + " " + recall + " " + overallFocus);

		vm.setTopicFocus(topicFocus);
		vm.setRecallOverAll(recall);
		vm.setOverallFocus(overallFocus);

		VMrepository.save(vm);

	}

	@Override
	protected void execute() throws WorkerException {

		// load users that are in twitter metadata table
		ConfigurationBuilder builder = super.getConfigurationBuilder();
		Twitter twitter = new TwitterFactory(builder.build()).getInstance();
		List<String> users = TMrepository.findAllForValidation();
		List<String> existingUsers = Vrepository.findAllScreenNames();

		// set up dates
		setDates();

		// delete data that is older than 3 months
		Vrepository.deleteOlderThan(new Date(System.currentTimeMillis() - (monthsBackwardsInDays * DAY_IN_MS)));
		
		int i = 0;
		
		// get all tweets between dates. stored in validation
		for (String user : users) {
			badUser = false;
			updateUser = false;
			
			i++;
			
			if (!existingUsers.contains(user)) {
				//System.out.println("months needed");
				fetchAndSaveAll(twitter, user, 833457172612837376L, 857613934651744258L);
				if(!badUser) {
					saveValidation(twitter, Urepository.findByScreenname(user));
				}
			} else {
				//System.out.println("day needed");
				fetchAndSaveAll(twitter, user, yesterday, today);
				if(!badUser) {
					updateUser = true;
					saveValidation(twitter, Urepository.findByScreenname(user));
				}
			}
		}

	}
}
