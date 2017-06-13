package br.les.opus.dengue.crawler;

public class SevereCrawlingException extends WorkerException {

	private static final long serialVersionUID = -5880333348314751037L;

	public SevereCrawlingException(String message, Throwable cause) {
		super(message, cause);
	}

	public SevereCrawlingException(String message) {
		super(message);
	}

	public SevereCrawlingException(Throwable cause) {
		super(cause);
	}
	
}
