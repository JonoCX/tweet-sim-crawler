package br.les.opus.dengue.crawler;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractWorker extends Thread {

	@Autowired
	private SessionFactory sessionFactory;
	
	protected Logger logger = Logger.getLogger(getClass());
	
	@Override
	public void run() {
		try {
			Session session = sessionFactory.getCurrentSession();
			session.beginTransaction();
			this.execute();
		} catch(Exception ex) {
			logger.error(ex.getMessage(), ex);
		} finally {
			Session session = sessionFactory.getCurrentSession(); 
			
			Transaction t = session.getTransaction(); 
			if (t != null && t.isActive()) {
				t.commit();
			}
			
			if (session.isOpen()) {
				session.close();
			}
		}
	}
	
	protected void saveChanges() {
		Session session = sessionFactory.getCurrentSession();
		session.getTransaction().commit();
		sessionFactory.getCurrentSession().beginTransaction();
	}
	
	protected abstract void execute() throws WorkerException;
}
