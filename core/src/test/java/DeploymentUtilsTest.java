import au.com.agic.deploymentsync.core.deployment.DeploymentUtils;
import au.com.agic.deploymentsync.core.deployment.impl.DeploymentUtilsImpl;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests of the deployment name santisier
 */
public class DeploymentUtilsTest {
	private static final DeploymentUtils DEPLOYMENT_UTILS = new DeploymentUtilsImpl();

	@Test
	public void testStringSantitise1() {
		Assert.assertEquals("api%231.0.war",
			DEPLOYMENT_UTILS.sanitizeName("api#1.0##12218734512865.war"));
		Assert.assertEquals("api%231.0.ear",
			DEPLOYMENT_UTILS.sanitizeName("api#1.0##12218734512865.ear"));
		Assert.assertEquals("api-feature.war",
			DEPLOYMENT_UTILS.sanitizeName("api-feature##12218734512865.war"));
		Assert.assertEquals("api%231.0-feature.war",
			DEPLOYMENT_UTILS.sanitizeName("api#1.0-feature##12218734512865.war"));
	}
}
