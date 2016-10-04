package au.com.agic.deploymentsync.core.exceptions;

public class UnknownStabilityException extends RuntimeException {

	private static final long serialVersionUID = -3564895847170919657L;

	public UnknownStabilityException(final String message) {
		super(message);
	}
}
