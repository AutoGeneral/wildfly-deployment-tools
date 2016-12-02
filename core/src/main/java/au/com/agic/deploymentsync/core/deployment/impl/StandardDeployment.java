package au.com.agic.deploymentsync.core.deployment.impl;

import au.com.agic.deploymentsync.core.deployment.Deployment;
import au.com.agic.deploymentsync.core.deployment.DeploymentUtils;

import org.apache.commons.codec.binary.Hex;

import java.util.Arrays;

/**
 * Basic implementation of Deployment interface
 */
public class StandardDeployment implements Deployment {

	private static final DeploymentUtils DEPLOYMENT_UTILS = new DeploymentUtilsImpl();

	private final String name;
	private byte[] sha1;
	private Boolean forceFlag = false;
	private String localPath;
	private Long enabledTime;

	public StandardDeployment(final String name) {
		this.name = name;
	}

	public StandardDeployment(final String name, final byte... sha1) {
		this.name = sanitizeName(name);
		this.sha1 = sha1;
	}

	public StandardDeployment(final String name, final byte[] sha1, final String localPath) {
		this.name = sanitizeName(name);
		this.sha1 = sha1;
		this.localPath = localPath;
	}

	@Override
	public final byte[] getSha1() {
		return sha1;
	}

	@Override
	public final String getLocalPath() {
		return localPath;
	}

	@Override
	public final void setLocalPath(final String localPath) {
		this.localPath = localPath;
	}

	@Override
	public final String getName() {
		return name;
	}

	@Override
	public final Boolean getForceFlag() {
		return forceFlag;
	}

	@Override
	public final void setForceFlag(final Boolean forceFlag) {
		this.forceFlag = forceFlag;
	}

	@Override
	public Long getEnabledTime() {
		return enabledTime;
	}

	@Override
	public void setEnabledTime(final Long enabledTime) {
		this.enabledTime = enabledTime;
	}

	/**
	 * Strip characters from deployment filename that can break things
	 * in Wildfly
	 * @param str string to sanitize
	 * @return sanitized string
	 */
	private String sanitizeName(final String str) {
		return DEPLOYMENT_UTILS.sanitizeName(str);
	}

	@Override
	public final boolean equals(final Object object) {
		if (object instanceof Deployment) {
			final Deployment deployment = (Deployment) object;
			return Hex.encodeHexString(getSha1()).equals(Hex.encodeHexString(deployment.getSha1()));
		}
		return false;
	}

	@Override
	public final int hashCode() {
		int result = name.hashCode();
		result += Arrays.hashCode(sha1);
		return result;
	}

}
