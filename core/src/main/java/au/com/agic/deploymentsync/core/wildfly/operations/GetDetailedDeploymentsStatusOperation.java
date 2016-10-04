package au.com.agic.deploymentsync.core.wildfly.operations;

import org.jboss.as.cli.Util;
import org.jboss.dmr.ModelNode;

/**
 * Wildfly Native API get details info about deployments operation constructor
 * This operation returns an object with deployments resources
 */
public class GetDetailedDeploymentsStatusOperation extends ModelNode {

	private static final long serialVersionUID = 3420314156274489993L;
	public static final String SERVER_NAME = "main-server";
	public static final String RECURSIVE_FLAG = "recursive";

	public GetDetailedDeploymentsStatusOperation(final String hostName) {
		get(Util.OPERATION).set(Util.READ_CHILDREN_RESOURCES);
		get(Util.CHILD_TYPE).set(Util.DEPLOYMENT);
		get(Util.INCLUDE_RUNTIME).set(true);
		get(RECURSIVE_FLAG).set(true);

		ModelNode address = get(Util.ADDRESS);
		address.add(Util.HOST);
		address.add(hostName);
		address.add(Util.SERVER);
		address.add(SERVER_NAME);
	}
}
