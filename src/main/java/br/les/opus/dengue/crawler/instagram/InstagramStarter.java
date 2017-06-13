package br.les.opus.dengue.crawler.instagram;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import br.les.opus.dengue.crawler.AbstractWorker;
import br.les.opus.dengue.crawler.WorkerException;
import br.les.opus.twitter.domain.HashTag;
import br.les.opus.twitter.repositories.HashTagRepository;

@Component
public class InstagramStarter extends AbstractWorker {
	
	@Autowired
	private SessionFactory sessionFactory;
	
	@Autowired
	private ApplicationContext context;
	
	@Autowired
	private HashTagRepository hashTagRepository;

	@Override
	protected void execute() throws WorkerException {
		Session current = sessionFactory.getCurrentSession();
		current.beginTransaction();

		for (HashTag tag : hashTagRepository.findAllActive()) {
			InstagramCrawler newInstagramCrawler = (InstagramCrawler)context.getBean("instagramCrawler");
			newInstagramCrawler.setHashTag(tag.getText());
			newInstagramCrawler.start();
		}
		
		current.close();
	}

}
