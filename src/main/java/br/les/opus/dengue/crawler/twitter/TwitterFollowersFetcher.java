package br.les.opus.dengue.crawler.twitter;

import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.les.opus.dengue.crawler.WorkerException;
import br.les.opus.twitter.domain.TwitterUser;
import br.les.opus.twitter.repositories.TwitterUserRepository;
import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

@Component
@Transactional
public class TwitterFollowersFetcher extends TwitterWorker {

	@Autowired
	private TwitterUserRepository userDao;

	protected Twitter getTwitterApi() {
		ConfigurationBuilder builder = getConfigurationBuilder();
		TwitterFactory f = new TwitterFactory(builder.build());
		return f.getInstance();
	}

	private void storeFollowers(TwitterUser user, IDs followers) {
		long[] ids = followers.getIDs();
		for (Long followerId : ids) {
			TwitterUser follower = userDao.findOne(followerId);
			if (follower == null) {
				follower = new TwitterUser(followerId);
				follower.setOutdatedFollowers(false);
				follower = userDao.save(follower);
			}
			user.getFollowers().add(follower);
		}
		user.setOutdatedFollowers(false);
		userDao.save(user);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	protected IDs fetchAndStoreFollowers(TwitterUser user) throws TwitterException {
		Twitter twitter = this.getTwitterApi();
		long cursor = -1;
		IDs ids = null;
		do {
			ids = twitter.getFollowersIDs(user.getId(), cursor);
			System.out.println("Got a followers batch");
			cursor = ids.getNextCursor();
			this.storeFollowers(user, ids);
			System.out.println("Saved!");
		} while (ids.hasNext());
		logger.info("Finished fetching followers from " + user);
		return ids;
	}

	@Override
	protected void execute() throws WorkerException {
		List<TwitterUser> users = userDao.findAllWithOutdatedFollowers();
		Twitter twitter = this.getTwitterApi();
		try {
			int sec = (twitter.getRateLimitStatus().get("/followers/ids").getSecondsUntilReset() + 30) * 1000;
			if(sec > 0) {
			System.out.println("sleeping for: " + sec/1000);
			Thread.sleep(sec);
			}
		for (TwitterUser user : users) {
			try {
				logger.info("Fetching followers of " + user);
				this.fetchAndStoreFollowers(user);
				user.setOutdatedFollowers(false);
				userDao.save(user);
			}catch(TwitterException e) {
				if(e.getErrorCode() != 88) {
					System.out.println("bad user");
					user.setOutdatedFollowers(false);
					userDao.save(user);
				}
				else {
					throw new IllegalArgumentException();
				}
			}
		}
	}catch(Exception e) {
		System.out.println("caught");
//		if(e.getErrorCode() == 88) {
//			try {
//				//int sec = (twitter.getRateLimitStatus().get("/followers/ids").getSecondsUntilReset() + 30) * 1000;
//				//if(sec > 0) {
//				//System.out.println("sleeping for: " + sec/1000);
//				//Thread.sleep(sec);
//				System.exit(0);
//				
//			} catch (Exception e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//		}
	}
	}

}