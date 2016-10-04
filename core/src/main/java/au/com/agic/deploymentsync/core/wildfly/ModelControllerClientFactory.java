package au.com.agic.deploymentsync.core.wildfly;

import au.com.agic.deploymentsync.core.constants.Constants;

import org.jboss.as.controller.client.ModelControllerClient;

import java.net.InetAddress;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

public final class ModelControllerClientFactory {

	private ModelControllerClientFactory() {
	}

	/**
	 * Create a Wildfly domain controller connection object
	 *
	 * @param host     host address
	 * @param port     Wildfly's management port
	 * @param username Wildfly's management user name
	 * @param password Wildfly's management user password
	 * @return client object
	 */
	public static ModelControllerClient createModelControllerClient(
		final InetAddress host, final int port, final String username, final String password) {

		// Authorisation callback
		final CallbackHandler callbackHandler = callbacks -> {
			for (Callback current : callbacks) {
				if (current instanceof NameCallback) {
					NameCallback ncb = (NameCallback) current;
					ncb.setName(username);
				} else if (current instanceof PasswordCallback) {
					PasswordCallback pcb = (PasswordCallback) current;
					pcb.setPassword(password.toCharArray());
				} else if (current instanceof RealmCallback) {
					RealmCallback rcb = (RealmCallback) current;
					rcb.setText(rcb.getDefaultText());
				} else {
					throw new UnsupportedCallbackException(current);
				}
			}
		};
		return ModelControllerClient.Factory
			.create(Constants.WILDFLY_CLIENT_PROTOCOL, host, port, callbackHandler);
	}
}
