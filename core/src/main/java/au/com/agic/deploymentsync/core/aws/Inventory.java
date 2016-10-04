package au.com.agic.deploymentsync.core.aws;

import com.amazonaws.services.ec2.model.Instance;

import java.util.List;

/**
 * AWS Inventory class
 */
public interface Inventory {

	/**
	 * Returns a list of instances filtered by tag name/value
	 *
	 * @param tagName  tag name to filter
	 * @param tagValue tag value to filter
	 * @return list of instances
	 */
	List<Instance> getInstancesByTag(final String tagName, final String tagValue);

	/**
	 * Returns a list of instances with Wildfly domain controllers
	 *
	 * @return list of instances
	 */
	List<Instance> getWildflyDomainControllers();
}
