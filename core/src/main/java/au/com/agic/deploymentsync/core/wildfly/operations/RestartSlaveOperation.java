package au.com.agic.deploymentsync.core.wildfly.operations;

import org.jboss.as.cli.Util;
import org.jboss.dmr.ModelNode;

/**
 * Wildfly Native API restart slave operation constructor
 * This operation returns an object with result of the restart command execution
 */
public class RestartSlaveOperation extends ModelNode {

	private static final long serialVersionUID = 71764657698511757L;
	public static final String SERVER_NAME = "main-server";
	public static final String SERVER_CONFIG = "server-config";

	public RestartSlaveOperation(final String hostName) {
		get(Util.OPERATION).set(Util.RESTART);
		ModelNode address = get(Util.ADDRESS);
		address.add(Util.HOST);
		address.add(hostName);
		address.add(SERVER_CONFIG);
		address.add(SERVER_NAME);
	}
}
