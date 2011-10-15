package socio;

import org.apache.log4j.Logger;

import socio.rest.RestLauncher;
import socio.tray.Tray;
import socio.xmpp.XmppClient;

/**
 * Launcher class for SocIO. Takes command line parameters, triggers
 * {@link Config} and starts all components accordingly.
 * 
 * @author th
 * 
 */
public class Launcher {
	private static Logger logger = Logger.getLogger(Launcher.class);

	public static void main(String[] args) {
		logger.info("T.H. SocIO Semantic Resource Manager");
		try {
			Config.getInstance().parseCommandline(args);

			if (!Config.isHeadless())
				new RestLauncher().bringUpRestApi(Config.getRestPort());

			if (!Config.isOffline()) {
				// Trigger XMPP client
				XmppClient.getInstance().bringUpClient(Config.getUserName(), Config.getPassword());
			}

			logger.info("SocIO is now operational!");

			if (UpdateChecker.updateAvailable()) {
				if (!Config.isHeadless()) {
					Tray.getInstance().notifyForUpdate();
				}
			}

			if (Config.isHeadless()) {
				// Prevent shutdown by waiting for any Console.in
				logger.info("Say anything at System.in to trigger shut down.");
				System.in.read();
				logger.info("Received something, shutting down.");
			} else {
				Tray.getInstance();
			}

		} catch (Exception e) {
			logger.error("Could not start SocIO", e);
			logger.error("Terminating SocIO because of errors in the initialization process.");
			System.exit(1);
		}
	}

}
