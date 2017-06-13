package br.les.opus.dengue.crawler.instagram;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import br.les.opus.dengue.crawler.AbstractWorker;
import br.les.opus.dengue.crawler.SevereCrawlingException;
import br.les.opus.dengue.crawler.WorkerException;
import br.les.opus.instagram.api.InstagramClient;
import br.les.opus.instagram.api.InstagramClientBuilder;
import br.les.opus.instagram.api.InstagramException;
import br.les.opus.instagram.api.endpoints.TagService;
import br.les.opus.instagram.domain.Media;
import br.les.opus.instagram.domain.MediaTagEnvelope;
import br.les.opus.instagram.services.MediaService;

@Component
@Scope("prototype")
@Transactional
public class InstagramCrawler extends AbstractWorker {

	@Autowired
	private Environment env;

	@Autowired
	private MediaService mediaService;

	private String hashTag;

	private boolean hasAllKnownMedia(List<Media> medias) {
		boolean wellKnowPoint = true;
		for (Media media : medias) {
			if (!mediaService.existis(media)) {
				wellKnowPoint = false;
				break;
			}
		}
		return wellKnowPoint;
	}
	
	public void saveMedias(List<Media> medias) {
		boolean hasKnown = false;
		if (hasAllKnownMedia(medias)) {
			hasKnown = true;
		}
		mediaService.save(medias);
		this.saveChanges();
		if (hasKnown) {
			throw new InstagramException("All page Media already in database. Stopping crawling...");
		}
	}
	
	public void setHashTag(String hashTag) {
		this.hashTag = hashTag;
	}
	
	private void crawlBackwards(MediaTagEnvelope envelope, TagService tagService) throws InterruptedException {
		Integer waitTime = Integer.parseInt(env.getProperty("instagram.crawl.backwards.waitTime"));
		while (envelope.getPagination().hasNextPage()) {
			String nextMaxTagId = envelope.getPagination().getNextMaxTagId();
			envelope = tagService.findAllRecentMediaByTag(this.hashTag, nextMaxTagId);
			logger.info(envelope.getData().size() + " mídias recebidas #" + hashTag);
			this.saveMedias(envelope.getData());
			logger.info("mídias salvas #" + hashTag);
			Thread.sleep(waitTime);
		}
	}

	@Override
	public void execute() throws WorkerException {
		logger.info("Iniciando craw do instagram #" + hashTag);
		String clientId = env.getProperty("instagram.client.id");
		InstagramClient client = new InstagramClientBuilder().clientId(clientId).build();
		
		if (hashTag == null || hashTag.isEmpty()) {
			throw new SevereCrawlingException("No hashtag defined! Aborting...");
		}

		TagService tagService = client.getTagService();
		MediaTagEnvelope envelope = tagService.findAllRecentMediaByTag(hashTag);
		logger.info(envelope.getData().size() + " mídias recebidas #" + hashTag);
		this.saveMedias(envelope.getData());
		logger.info("mídias salvas #" + hashTag);
		
		try {
			/**
			 * If there is another page, start to crawl backwards until find a already
			 * stored post
			 */
			if (envelope.getPagination() != null && envelope.getPagination().hasNextPage()) {
				this.crawlBackwards(envelope, tagService);
			}
		} catch (InterruptedException e) {
			logger.error("Error during backwards crawling", e);
		}
	}
}
