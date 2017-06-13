package br.les.opus.dengue.crawler.twitter;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.les.opus.twitter.domain.HashTag;
import br.les.opus.twitter.repositories.HashTagRepository;
import twitter4j.FilterQuery;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;


@Component
@Transactional
public class TwitterCrawler extends TwitterWorker {

	@Autowired
	private StatusListener listener;

	@Autowired
	private HashTagRepository repository;

	protected List<HashTag> getHashTags() {
		return repository.findAllActive();
	}

	protected String[] getKeyWords() {
		List<HashTag> tags = repository.findAllActive();
		String[] tagsArray = new String[tags.size()];
		int i = 0;
		for (HashTag hashTag : tags) {
			tagsArray[i++] = hashTag.getText();
		}
		return tagsArray;
	}

	@Override
	public void execute() {
		ConfigurationBuilder builder = super.getConfigurationBuilder();
		TwitterStream twitterStream = new TwitterStreamFactory(builder.build()).getInstance();
		FilterQuery fq = new FilterQuery();
		String keywords[] = this.getKeyWords();
		String keys = " ";
		for(String s : keywords) {
			keys += s + " ";
		}
		System.out.print("Tracking keywords: " + keys);
		fq.track(keywords);
		fq.language(new String[]{"pt"});
		twitterStream.addListener(listener);
		twitterStream.filter(fq);
	}

}

