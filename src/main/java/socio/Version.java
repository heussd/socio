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
	private String version;

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
		checkForUpdates();
	}

	public boolean updateAvailable() {
		return updateAvailable;
	}

	public void checkForUpdates() {
		Properties packagedProperties = new Properties();
		Properties gitHubProperties = new Properties();
		
		try {
			packagedProperties.load(Version.class.getClassLoader().getResourceAsStream("version.properties"));
			gitHubProperties.load(new URL(GITHUB_VERSION_PROPERTIES).openStream());

			version = packagedProperties.getProperty("version");
			LOGGER.info("This is version #" + version);

			if (!packagedProperties.get("version").equals(gitHubProperties.get("version"))) {
				LOGGER.warn("New version available: Version #" + gitHubProperties.get("version"));
				LOGGER.warn("Please always use the newest stable version!");
				updateAvailable = true;
			}
		} catch (Exception e) {
			LOGGER.error("Could not check for updates:", e);
		}
		LOGGER.debug("No update available");
		updateAvailable = false;
	}
}
