package au.com.agic.deploymentsync.core.wildfly;

import au.com.agic.deploymentsync.core.deployment.Deployment;

import org.jboss.dmr.ModelNode;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DomainControllerClient {

	/**
	 * @return domain controller address
	 */
	InetAddress getServerAddress();

	/**
	 * @return hostnames for Wildfly domain slaves
	 */
	List<String> getSlavesHostnames();

	/**
	 * @param ipAddresses Wildfly domain slaves IP addresses
	 * @return list of deployments deployed to slave and active
	 */
	List<Deployment> getDeployments(List<String> ipAddresses);

	/**
	 * @param deployment application's deployment to deploy
	 * @return result of operation
	 */
	Optional<ModelNode> deploy(Deployment deployment);

	/**
	 * @param deployment application's deployment to undeploy
	 * @return result of operation
	 */
	Optional<ModelNode> undeploy(Deployment deployment, String serverGroup);

	/**
	 * Gets information from slaves and returns their actual statuses
	 * when key is slave hostname and value is their status: true - active, false - soemthing wrong
	 * @return slave statuses
	 */
	Map<String, Boolean> getSlaveActivityStatuses();

	/**
	 * Restarts slave
	 * @param hostname slave's hostname
	 * @return result of the opertaion
	 */
	Optional<ModelNode> restartSlave(String hostname);

	/**
	 * @param client     Wildfly domain controller client interface
	 * @param deployment thing to deploy
	 * @return was that deployment already deployed to domain controller
	 */
	boolean hasDeployment(Deployment deployment);

	/**
	 * Close connection with remote Wildfly domain controller
	 */
	void close();
}
