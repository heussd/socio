package socio;

import org.apache.log4j.Logger;

import socio.observer.PocketComPublisher;
import socio.rest.RestLauncher;
import socio.semantic.SemanticCore;
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
			Tray.getInstance();
			Version.getInstance();

			if (!Config.isHeadless())
				new RestLauncher().bringUpRestApi(Config.getRestPort());

			if (!Config.isOffline()) {
				// Trigger XMPP client
				XmppClient.getInstance().bringUpClient(Config.getUserName(), Config.getPassword());
			}

			SemanticCore.getInstance();

			logger.info("Registering observers...");
			try {
				SemanticCore.getInstance().addObserver(new PocketComPublisher());
			} catch (Exception e) {
				logger.error("Could not start observer", e);
			}

			logger.info("SocIO is now operational!");

			if (Config.isHeadless()) {
				// Prevent shutdown by waiting for any Console.in
				logger.info("Say anything at System.in to trigger shut down.");
				System.in.read();
				logger.info("Received something, shutting down.");
			} else {
				Thread.sleep(1000);
				if (Version.getInstance().updateAvailable())
					Tray.getInstance().notifyForUpdate();
			}

		} catch (Exception e) {
			logger.error("Could not start SocIO", e);
			logger.error("Terminating SocIO because of errors in the initialization process.");
			System.exit(1);
		}
	}

}
