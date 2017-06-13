package br.les.opus.dengue.crawler.twitter;


import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import br.les.opus.twitter.domain.Tweet;
import br.les.opus.twitter.repositories.TweetRepository;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;

@Component
public class TweetListener implements StatusListener {
	
	@Autowired
	private TweetRepository repository;
	
	@Autowired
	private SessionFactory sessionFactory;
	
	private Logger logger;
	
	private Gson gson;
	
	public TweetListener() {
		this.gson = new Gson();
		this.logger = Logger.getLogger(TweetListener.class);
	}

	@Override
	public void onException(Exception ex) {
		logger.error(ex.getMessage(), ex);
		System.exit(-1);
	}
	
	@Override
	@Transactional(value = TxType.REQUIRES_NEW)
	public void onStatus(Status status) {
		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();
		
		String json = gson.toJson(status);
		Tweet tweet = gson.fromJson(json, Tweet.class);
		if (status.getGeoLocation() != null) {
			tweet.setGeolocation(status.getGeoLocation());
		}
		repository.save(tweet);
		logger.info(tweet);
		session.getTransaction().commit();
	}
	
	@Override
	public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
		
	}

	@Override
	public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
		
	}

	@Override
	public void onScrubGeo(long userId, long upToStatusId) {
		
	}

	@Override
	public void onStallWarning(StallWarning warning) {
		
	}
	
}
