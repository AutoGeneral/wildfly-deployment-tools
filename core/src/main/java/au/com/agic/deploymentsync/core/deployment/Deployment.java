package au.com.agic.deploymentsync.core.deployment;

/**
 * Represents application's deployment artifact
 */
public interface Deployment {

	byte[] getSha1();

	String getLocalPath();

	void setLocalPath(String localPath);

	String getName();

	Boolean getForceFlag();

	void setForceFlag(Boolean forceFlag);

	boolean equals(Object object);

	int hashCode();
}
