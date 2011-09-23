package socio;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class Config {

	/**
	 * Implementation of a thread safe, synchronized singleton pattern
	 * {@link http://de.wikibooks.org/wiki/Java_Standard:_Muster_Singleton}.
	 */
	private static Config instance;

	public static synchronized Config getInstance() {
		if (Config.instance == null) {
			Config.instance = new Config();
		}
		return Config.instance;
	}

	/**
	 * Overrides some values for the automated unit tests.
	 */
	public static synchronized Config getTestInstance() {
		Config.instance = new Config(true) {

			@Override
			public boolean isReadonly() {
				return false;
			}

			@Override
			public boolean disableTray() {
				return true;
			}

			@Override
			public String getXmppUserId() {
				return "xmpp://user@example.com";
			}
		};
		return Config.instance;
	}

	private static Logger logger = Logger.getLogger(Config.class);

	private static final String FILE_NAME = "socio.properties";

	private Properties properties;

	private Boolean headless;

	private Boolean debug;

	private Boolean readonly;

	private Boolean disableTray;

	private Boolean offline;

	/**
	 * TODO Clean this mess up!
	 */
	private Config() {
		this.properties = new Properties();
		loadConfig();

		while (!isValidXmppId(getXmppUserId())) {
			setupWizard();
		}

		logger.debug("XMPP ID = " + getXmppUserId());
		logger.debug("Password is " + ((getPassword() == null | getPassword().equals("")) ? "UNSET" : "set"));
		logger.debug("User name = " + getUserName());
		logger.debug("Server = " + getServerAddress());
		logger.debug("Use proxy = " + (useProxy() ? "true" : "false"));
		logger.debug("Is headless = " + (isHeadless() ? "true" : "false"));
		logger.debug("Debug mode = " + (isDebug() ? "true" : "false"));
	}

	private Config(Boolean debug) {
		logger.info("Config class is in test mode!");
	}

	private Boolean loadConfig() {
		try {
			properties.load(new FileInputStream(FILE_NAME));
			return true;
		} catch (Exception e) {
			logger.error("Could not load properties file " + FILE_NAME, e);
		}
		return false;
	}

	/**
	 * Basic user name validations
	 * 
	 * @return A valid user name / null
	 */
	public Boolean isValidXmppId(String userName) {
		if (userName == null)
			return false;

		if (!userName.startsWith("xmpp://"))
			return false;

		if (!(userName.split("@").length == 2))
			return false;

		return true;
	}

	private void setupWizard() {
		try {
			System.out.println("SocIO setup");
			this.properties = new Properties();
			String input = null;

			while (!isValidXmppId(input)) {
				System.out.print("Please input a valid XMPP id (in the form xmpp://user@example.com): ");
				byte[] buffer = new byte[100];
				System.in.read(buffer, 0, 99);

				input = (new String(buffer)).trim();
				logger.debug("User entered username \"" + input + "\"");
			}

			properties.setProperty("xmpp.user", input);
			input = "";

			while ("".equals(input)) {
				System.out.print("Please input an password: ");
				byte[] buffer = new byte[100];
				System.in.read(buffer, 0, 99);

				input = (new String(buffer)).trim();
				logger.debug("User entered a password");
			}

			properties.setProperty("xmpp.pass", input);

		} catch (Exception e) {
			logger.error("Could not retrieve user inputs:", e);
		}

		try {
			properties.store(new FileOutputStream(FILE_NAME), "SocIO Configuration File");
		} catch (Exception e) {
			logger.error("Could not store properties file " + FILE_NAME, e);
		}

		loadConfig();
	}

	public String getPassword() {
		String property = properties.getProperty("xmpp.pass");
		if (property == null | property.equals("")) {
			logger.warn("Property xmpp.pass is empty.");
		}
		return property;
	}

	public String getXmppUserId() {
		String user = properties.getProperty("xmpp.user");

		if (!isValidXmppId(user))
			logger.warn("Invalid user name: " + user);
		return user;
	}

	public String getServerAddress() {
		return getXmppUserId().split("@")[1];
	}

	public String getUserName() {
		return getXmppUserId().split("@")[0].replaceAll("xmpp://", "");
	}

	public boolean useProxy() {
		return properties.getProperty("proxy.address") != null;
	}

	public String getProxyAddress() {
		return properties.getProperty("proxy.address");
	}

	public int getServerPort() {
		// TODO: Hardcoded: Port config setting is hardcoded to 5222.
		return 5222;
	}

	public int getProxyPort() {
		// TODO Hardcoded: Proxy Port config setting is hardcoded to 80.
		return 80;
	}

	public Boolean isHeadless() {
		if (headless == null) {
			String headlessString = properties.getProperty("headless");

			if (headlessString == null)
				headless = false;

			headless = "true".equals(headlessString);
		}
		return headless;
	}

	public Boolean isDebug() {
		if (debug == null) {
			String debugString = properties.getProperty("debug");

			if (debugString == null)
				debug = false;

			debug = "true".equals(debugString);
		}
		return debug;
	}

	public void takeParameters(String[] args) {
		for (String parameter : args) {
			if ("headless".equals(parameter)) {
				logger.info("Manual override: Headless mode enabled.");
				headless = true;
			} else if ("debug".equals(parameter)) {
				logger.info("Manual override: Debug mode enabled.");
				debug = true;
			} else if ("readonly".equals(parameter)) {
				logger.info("Manual override: Readonly mode enabled.");
				readonly = true;
			} else {
				logger.warn("Unrecognised parameter " + parameter + " was ignored.");
			}

		}
	}

	public boolean isReadonly() {
		if (readonly == null) {
			String string = properties.getProperty("readonly");

			if (string == null)
				readonly = false;

			readonly = "true".equals(string);
		}
		return readonly;
	}

	public boolean isOffline() {
		if (offline == null) {
			String string = properties.getProperty("offline");

			if (string == null)
				offline = false;

			offline = "true".equals(string);
		}
		return offline;
	}

	public boolean disableTray() {
		if (disableTray == null) {
			String string = properties.getProperty("disabletray");

			if (string == null)
				disableTray = false;

			disableTray = "true".equals(string);
		}
		return disableTray;
	}

}
