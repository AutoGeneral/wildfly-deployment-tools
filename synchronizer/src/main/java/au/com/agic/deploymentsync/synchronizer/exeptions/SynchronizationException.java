package au.com.agic.deploymentsync.synchronizer.exeptions;

public class SynchronizationException extends RuntimeException {

	private static final long serialVersionUID = -4151757943570693430L;

	public SynchronizationException(final String message) {
		super(message);
	}
}
