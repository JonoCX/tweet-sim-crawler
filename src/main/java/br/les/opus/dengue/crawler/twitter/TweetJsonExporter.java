package br.les.opus.dengue.crawler.twitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vividsolutions.jts.geom.Point;

import br.les.opus.dengue.crawler.AbstractWorker;
import br.les.opus.dengue.crawler.WorkerException;
import br.les.opus.dengue.crawler.gson.IsoSimpleDateGsonSerializer;
import br.les.opus.dengue.crawler.gson.JtsPointGsonSerializer;
import br.les.opus.twitter.domain.Tweet;
import br.les.opus.twitter.repositories.TweetRepository;

@Component
@Transactional
public class TweetJsonExporter extends AbstractWorker {

	@Autowired
	private TweetRepository tweetRepository;
	
	private String language;
	
	private Gson gson;
	
	public TweetJsonExporter() {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Point.class, new JtsPointGsonSerializer());
		builder.registerTypeAdapter(Date.class, new IsoSimpleDateGsonSerializer());
		gson = builder.create();
	}
	
	private Set<Long> getIds() throws IOException {
		Set<Long> ids = new HashSet<>();
		BufferedReader reader = new BufferedReader(new FileReader(new File("ids")));
		String line = reader.readLine();
		while (line != null) {
			ids.add(Long.valueOf(line));
			line = reader.readLine();
		}
		reader.close();
		return ids;
	}
	
	private List<Tweet> findAllById() throws IOException {
		List<Tweet> selectedTweets = new ArrayList<>();
		Set<Long> ids  = this.getIds();
		for (Long id : ids) {
			Tweet tweet = tweetRepository.findOne(id);
			if (tweet != null) {
				selectedTweets.add(tweet);
			}
		}
		return selectedTweets;
	}
	
	@Override
	protected void execute() throws WorkerException {
		try {
			List<Tweet> selectedTweets = findAllById(); 
			System.out.println("Exportando " + selectedTweets.size() + " tweets");
			File jsonFile = new File("tweets.json");
			FileOutputStream fOut = new FileOutputStream(jsonFile);
			OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
			myOutWriter.append(gson.toJson(selectedTweets));
			myOutWriter.close();
			fOut.close();
		} catch (Exception e) {
			throw new WorkerException(e.getMessage(), e);
		}
	}
	
	public void setLanguage(String language) {
		this.language = language;
	}
	
	public String getLanguage() {
		return language;
	}

}
