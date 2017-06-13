package br.les.opus.dengue.crawler;

public class WorkerException extends Exception {

	private static final long serialVersionUID = -7819593848717506348L;

	public WorkerException(String message, Throwable cause) {
		super(message, cause);
	}

	public WorkerException(String message) {
		super(message);
	}

	public WorkerException(Throwable cause) {
		super(cause);
	}

	
}
