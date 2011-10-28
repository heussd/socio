package socio.tray;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.apache.log4j.Logger;

import socio.Config;

/**
 * http://java.sun.com/developer/technicalArticles/J2SE/Desktop/javase6/
 * systemtray/
 * 
 * @author th
 * 
 */
public class Tray {

	/**
	 * Implementation of a performance critical, synchronized singleton pattern
	 * {@link http://de.wikibooks.org/wiki/Java_Standard:_Muster_Singleton}.
	 */
	private static final class InstanceHolder {
		static Tray INSTANCE = new Tray();
	}

	public static Tray getInstance() {
		return InstanceHolder.INSTANCE;
	}

	private static final Logger LOGGER = Logger.getLogger(Tray.class);
	private TrayIcon trayIcon;

	private Tray() {
		if (!Config.disableTray() && SystemTray.isSupported()) {
			LOGGER.info("Bringing up tray...");

			SystemTray tray = SystemTray.getSystemTray();
			Image image = Toolkit.getDefaultToolkit().getImage(Tray.class.getClassLoader().getResource("socio_icon.png"));
			PopupMenu popup = new PopupMenu();

			MenuItem browseMenuItem = new MenuItem("Browse commits on GitHub...");
			browseMenuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent actionEvent) {
					if (java.awt.Desktop.isDesktopSupported()) {
						java.awt.Desktop desktop = java.awt.Desktop.getDesktop();

						if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
							try {
								java.net.URI uri = new java.net.URI("https://github.com/heussd/socio/commits/master");
								desktop.browse(uri);
							} catch (Exception e) {
								LOGGER.error("Could not browse resource", e);
							}
						} else {
							LOGGER.error("Action BROWSE not supported.");
						}
					} else {
						LOGGER.error("Desktop is not supported (fatal)");
					}

				}
			});

			MenuItem exitMenuItem = new MenuItem("Exit");
			exitMenuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					System.exit(0);
				}
			});

			popup.add(browseMenuItem);
			popup.add(exitMenuItem);

			trayIcon = new TrayIcon(image, "SocIO Tray Icon", popup);
			trayIcon.setImageAutoSize(true);

			try {
				tray.add(trayIcon);
				LOGGER.info("Tray is up...");
			} catch (AWTException e) {
				System.err.println("TrayIcon could not be added.");
			}

		} else {
			LOGGER.warn("Tray is disabled.");
		}
	}

	public void notifyForUpdate() {
		trayIcon.displayMessage("Update available", "There is an updated version of this program available - please update immediately.", MessageType.INFO);
	}

	/*
	 * This methods are used by the Log4J Tray Logappender.
	 */

	public void warn(String message) {
		if (trayIcon != null)
			trayIcon.displayMessage("Warning", message, MessageType.WARNING);
	}

	public void error(String message) {
		if (trayIcon != null)
			trayIcon.displayMessage("Error", message, MessageType.ERROR);
	}

}
