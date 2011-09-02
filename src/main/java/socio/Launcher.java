package socio;

import org.apache.log4j.Logger;

import socio.rest.RestLauncher;
import socio.semantic.SemanticCore;
import socio.semantic.Semantics;
import socio.xmpp.XmppClient;

/**
 * Launcher class for SocIO. Takes command line parameters, triggers
 * {@link Config} and starts all components accordingly.
 * 
 * FIXME: This would be a great place to start implementing exception handling
 * 
 * @author th
 * 
 */
public class Launcher {
	private static Logger logger = Logger.getLogger(Launcher.class);
	private static final String VERSION = "2011-09-02-10";

	private static Boolean headless;
	private static Boolean debug;

	public static void main(String[] args) {
		logger.info("T.H. SocIO Semantic Resource Manager Version " + VERSION);

		// Set flags to default according to config
		headless = Config.getInstance().isHeadless();
		debug = Config.getInstance().isDebug();

		// Allow the flags to be overwriten via commandline
		for (String parameter : args) {
			if ("headless".equals(parameter)) {
				logger.info("Manual override: Headless mode enabled.");
				headless = true;
			} else if ("debug".equals(parameter)) {
				logger.info("Manual override: Debug mode enabled.");
				debug = true;
			} else {
				logger.warn("Unrecognised parameter " + parameter + " was ignored.");
			}

		}

		try {

			if (!headless)
				new RestLauncher();

			// Early start of the semantic core when in debug mode
			if (debug) {
				// TODO: Use a dedicated rdf store for debug here
				SemanticCore.getInstance().clear().persistStatements(new Semantics().constructDemoMessageModel(), true);
			}

			XmppClient.getInstance();

			logger.info("SocIO is now operational!");
			
			if (headless) {
				// Prevent shutdown by waiting for any Console.in
				logger.info("Say anything at System.in to trigger shut down.");
				System.in.read();
				logger.info("Received something, shutting down.");
			}
			if (debug) {
				long timeLimit = System.currentTimeMillis() + 60000;
				while (true) {
					Thread.sleep(5000);
					if (System.currentTimeMillis() > timeLimit) {
						logger.info("Time limit hit, shutting down.");
						System.exit(0);
					} else {
						logger.info("Time limit will be hit in " + ((timeLimit - System.currentTimeMillis()) / 1000) + " seconds...");
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error(s) occured", e);
		}
	}

}
