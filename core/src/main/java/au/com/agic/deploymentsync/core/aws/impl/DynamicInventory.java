package au.com.agic.deploymentsync.core.aws.impl;

import au.com.agic.deploymentsync.core.configuration.CoreConfiguration;
import au.com.agic.deploymentsync.core.aws.ColourResolver;
import au.com.agic.deploymentsync.core.aws.Inventory;
import au.com.agic.deploymentsync.core.constants.Constants;
import au.com.agic.deploymentsync.core.constants.Defaults;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implementation of AWS Dynamic Inventory
 */
public class DynamicInventory implements Inventory {

	private static final Logger LOGGER = Logger.getLogger(DynamicInventory.class.getName());

	// AWS instance running code
	// http://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_InstanceState.html
	private static final int RUNNING_CODE = 16;

	private final AmazonEC2 ec2;
	private final String environment;
	private final String colour;

	public DynamicInventory(final CoreConfiguration configuration) {
		ColourResolver colourResolver = new StaticColourResolver();

		environment = configuration.getEnvironment();
		colour = colourResolver.getColourForStability(configuration.getStability());

		// Use credentials from arguments if possible, otherwise use default .aws/credentials
		if (configuration.getAwsCredentials() != null) {
			ec2 = new AmazonEC2Client(configuration.getAwsCredentials());
		} else {
			ec2 = new AmazonEC2Client();
		}
		ec2.setRegion(Defaults.AWS_REGION);
	}

	@Override
	public final List<Instance> getInstancesByTag(final String tagName, final String tagValue) {
		List<Instance> instances = getInstances();

		return instances.stream()
			// Filter instances by tag
			.filter(instance -> {
				List<Tag> tags = instance.getTags();
				for (Tag tag : tags) {
					if (tag.getKey().equals(tagName) && tag.getValue().equals(tagValue)) {
						return true;
					}
				}
				return false;
			})
			// Filter instances by state
			.filter(instance -> {
				final int instanceStateCode = instance.getState().getCode();
				if (instanceStateCode != RUNNING_CODE) {
					LOGGER.log(Level.INFO, instance.getInstanceId()
						+ " is matching " + tagName + ":" + tagValue
						+ " tag but it's not running. State: " + instance.getState());
				}
				return instanceStateCode == RUNNING_CODE;
			})
			.collect(Collectors.toList());
	}

	@Override
	public final List<Instance> getWildflyDomainControllers() {
		return getInstancesByTag(Constants.TAG_ROLE, Constants.ROLE_WILDFLY_DOMAIN_CONTROLLER);
	}

	/**
	 * Returns a list of EC2 instances filtered by Environment and Colour tags
	 *
	 * @return list of instances
	 */
	private List<Instance> getInstances() {
		List<Reservation> reservations = ec2.describeInstances().getReservations();
		List<Instance> result = new ArrayList<>();

		reservations.forEach(reservation -> {
			result.addAll(reservation.getInstances().stream().filter(instance -> {
				List<Tag> tags = instance.getTags();
				int flag = 0;

				for (Tag tag : tags) {
					if (tag.getKey().equals(Constants.TAG_ENVIRONMENT) && tag.getValue()
						.equalsIgnoreCase(environment)
						|| tag.getKey().equals(Constants.TAG_COLOUR) && tag.getValue()
						.equalsIgnoreCase(colour)) {
						flag++;
					}
				}
				return flag == 2;
			}).collect(Collectors.toList()));
		});

		return result;
	}
}
