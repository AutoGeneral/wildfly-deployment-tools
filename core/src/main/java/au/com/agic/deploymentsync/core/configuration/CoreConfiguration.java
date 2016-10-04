package au.com.agic.deploymentsync.core.configuration;

import com.amazonaws.auth.BasicAWSCredentials;

public interface CoreConfiguration {

	String getWildflyLogin();

	String getWildflyPassword();

	String getEnvironment();

	String getStability();

	BasicAWSCredentials getAwsCredentials();

	Boolean getDryRun();
}
