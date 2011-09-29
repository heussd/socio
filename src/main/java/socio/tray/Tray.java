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
		static final Tray INSTANCE = new Tray();
	}

	public static Tray getInstance() {
		return InstanceHolder.INSTANCE;
	}

	public static void start() {
		if (!Config.getInstance().isHeadless()) {
			getInstance();
		} else {
			LOGGER.warn("System is headless, will not display tray icon!");
		}
	}

	private static final Logger LOGGER = Logger.getLogger(Tray.class);

	private TrayIcon trayIcon;

	private Tray() {
		if (!Config.getInstance().disableTray() && SystemTray.isSupported()) {

			SystemTray tray = SystemTray.getSystemTray();
			Image image = Toolkit.getDefaultToolkit().getImage(Tray.class.getClassLoader().getResource("socio_icon.png"));

			ActionListener exitListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					System.out.println("Exiting...");
					System.exit(0);
				}
			};

			PopupMenu popup = new PopupMenu();
			MenuItem defaultItem = new MenuItem("Exit");
			defaultItem.addActionListener(exitListener);
			popup.add(defaultItem);

			trayIcon = new TrayIcon(image, "SocIO Tray Icon", popup);
			trayIcon.setImageAutoSize(true);

			try {
				tray.add(trayIcon);
			} catch (AWTException e) {
				System.err.println("TrayIcon could not be added.");
			}

		}
	}

	public void notifyForUpdate() {
		trayIcon.displayMessage("Update available", "There is an updated version of this program available - please update immediately.", MessageType.INFO);
	}

	/*
	 * This methods are used by the Log4J Tray Logappender.
	 */

	public void warn(String message) {
		trayIcon.displayMessage("Warning", message, MessageType.WARNING);
	}

	public void error(String message) {
		trayIcon.displayMessage("Error", message, MessageType.ERROR);
	}

}
