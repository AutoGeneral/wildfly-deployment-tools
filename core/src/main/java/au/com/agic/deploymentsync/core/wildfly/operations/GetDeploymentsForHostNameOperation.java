package au.com.agic.deploymentsync.core.wildfly.operations;

import org.jboss.as.cli.Util;
import org.jboss.dmr.ModelNode;

/**
 * Wildfly Native API get deployments operation constructor
 * This operation returns an object with deployments resources
 */
public class GetDeploymentsForHostNameOperation extends ModelNode {

	private static final long serialVersionUID = -235110267598646687L;
	public static final String SERVER_NAME = "main-server";
	public static final String RECURSIVE_FLAG = "recursive";

	public GetDeploymentsForHostNameOperation(final String hostName) {
		get(Util.OPERATION).set(Util.READ_CHILDREN_RESOURCES);
		get(Util.CHILD_TYPE).set(Util.DEPLOYMENT);
		get(RECURSIVE_FLAG).set(true);

		ModelNode address = get(Util.ADDRESS);
		address.add(Util.HOST);
		address.add(hostName);
		address.add(Util.SERVER);
		address.add(SERVER_NAME);
	}
}
