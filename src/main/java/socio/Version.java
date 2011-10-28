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
public class Version {

	private static final Logger LOGGER = Logger.getLogger(Version.class);
	private static final String GITHUB_VERSION_PROPERTIES = "https://raw.github.com/heussd/socio/" + Config.getGitBranch() + "/src/main/resources/version.properties";

	private boolean updateAvailable;
	private Properties packagedProperties;

	/**
	 * Implementation of a thread safe, synchronized singleton pattern
	 * {@link http://de.wikibooks.org/wiki/Java_Standard:_Muster_Singleton}.
	 */
	private static Version instance;

	public static synchronized Version getInstance() {
		if (Version.instance == null) {
			Version.instance = new Version();
		}
		return Version.instance;
	}

	private Version() {
		packagedProperties = new Properties();
		try {
			packagedProperties.load(Version.class.getClassLoader().getResourceAsStream("version.properties"));
			LOGGER.info("This is version #" + getVersion());

		} catch (Exception e) {
			LOGGER.error("Could not load version information", e);
		}

		if (!Config.isOffline()) {
			checkForUpdates();
		} else {
			LOGGER.warn("Will not check for updates in OFFLINE mode!");
		}
	}

	public String getVersion() {
		return packagedProperties.getProperty("version");
	}

	public boolean updateAvailable() {
		return updateAvailable;
	}

	public void checkForUpdates() {
		if (!Config.isOffline()) {
			LOGGER.debug("Performing update check...");
			Properties gitHubProperties = new Properties();

			try {
				gitHubProperties.load(new URL(GITHUB_VERSION_PROPERTIES).openStream());

				if (!packagedProperties.get("version").equals(gitHubProperties.get("version"))) {
					LOGGER.info("New version available: Version #" + gitHubProperties.get("version"));
					updateAvailable = true;
				} else {
					LOGGER.debug("No update available");
					updateAvailable = false;
				}
			} catch (Exception e) {
				LOGGER.error("Could not check for updates:", e);
			}
		}
	}
}
