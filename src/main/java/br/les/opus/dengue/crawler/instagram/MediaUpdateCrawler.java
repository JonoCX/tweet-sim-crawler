package br.les.opus.dengue.crawler.instagram;


import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import br.les.opus.dengue.crawler.AbstractWorker;
import br.les.opus.dengue.crawler.WorkerException;
import br.les.opus.instagram.api.InstagramClient;
import br.les.opus.instagram.api.InstagramClientBuilder;
import br.les.opus.instagram.api.endpoints.MediaEndpointService;
import br.les.opus.instagram.domain.Media;
import br.les.opus.instagram.repository.MediaRepository;
import br.les.opus.instagram.repository.PicturePoolRepository;
import br.les.opus.instagram.repository.VideoPoolRepository;

@Component
@Scope("prototype")
@Transactional
public class MediaUpdateCrawler extends AbstractWorker {

	@Autowired
	private Environment env;

	@Autowired
	private MediaRepository mediaRepository;
	
	@Autowired
	private PicturePoolRepository picPoolDao;
	
	@Autowired
	private VideoPoolRepository videoPoolDao;

	@Override
	public void execute() throws WorkerException {
		List<Media> medias = mediaRepository.findAll();
		logger.info("Iniciando craw de dados adicionais de todas as mídias");
		String clientId = env.getProperty("instagram.client.id");
		InstagramClient client = new InstagramClientBuilder().clientId(clientId).build();

		for (Media media : medias) {
			if (media.isValid()) {
				continue;
			}
			MediaEndpointService mediaEndpoint = client.getMediaService();
			Media downloadedMedia = mediaEndpoint.findById(media.getId());
			if (downloadedMedia != null) {
				downloadedMedia.replaceImages(media.getImages());
				downloadedMedia.replaceVideos(media.getVideos());
				
				if (downloadedMedia.getImages() != null) {
					picPoolDao.save(downloadedMedia.getImages());
				}
				if (downloadedMedia.getVideos() != null) {
					videoPoolDao.save(downloadedMedia.getVideos());
				}
				
				logger.info("Midia atualizada: " + media.getId());
				saveChanges();
			} else {
				logger.warn("Midia já removida do servidor :/ " + media.getId());
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
