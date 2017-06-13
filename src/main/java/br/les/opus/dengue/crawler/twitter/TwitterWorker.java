package br.les.opus.dengue.crawler.twitter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import br.les.opus.dengue.crawler.AbstractWorker;
import twitter4j.conf.ConfigurationBuilder;

public abstract class TwitterWorker extends AbstractWorker {

	@Autowired
	private Environment env;

	protected ConfigurationBuilder getConfigurationBuilder() {
		String consumerKey = env.getProperty("twitter.consumer.key");
		String consumerSecret = env.getProperty("twitter.consumer.secret");
		String token = env.getProperty("twitter.token");
		String tokenSecret = env.getProperty("twitter.token.secret");

		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true);
		cb.setOAuthConsumerKey(consumerKey);
		cb.setOAuthConsumerSecret(consumerSecret);
		cb.setOAuthAccessToken(token);
		cb.setOAuthAccessTokenSecret(tokenSecret);
		
		return cb;
	}
}
