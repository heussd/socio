package socio;

import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Checks for available updates on GitHub.
 * 
 * TODO: Make the check mechanism asynchronous.
 * 
 * @author th
 * 
 */
public class UpdateChecker {

	private static final Logger LOGGER = Logger.getLogger(UpdateChecker.class);
	private static final String GITHUB_VERSION_PROPERTIES = "https://raw.github.com/heussd/socio/master/src/main/resources/version.properties";

	public static boolean updateAvailable() {
		Properties packagedProperties = new Properties();
		Properties gitHubProperties = new Properties();

		// Move this in the central config class
		if (Config.useProxy()) {
			System.setProperty("proxyPort", "" + Config.getProxyPort());
			System.setProperty("proxyHost", Config.getProxyAddress());
		}

		try {
			packagedProperties.load(UpdateChecker.class.getClassLoader().getResourceAsStream("version.properties"));
			gitHubProperties.load(new URL(GITHUB_VERSION_PROPERTIES).openStream());

			LOGGER.debug("This is version #" + packagedProperties.get("version"));

			if (!packagedProperties.get("version").equals(gitHubProperties.get("version"))) {
				LOGGER.warn("New version available: Version #" + gitHubProperties.get("version"));
				LOGGER.warn("Please always use the newest stable version!");
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("Could not check for updates:", e);
		}
		LOGGER.debug("No update available");
		return false;
	}
}
