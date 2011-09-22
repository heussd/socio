package socio;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * http://java.sun.com/developer/technicalArticles/J2SE/Desktop/javase6/
 * systemtray/
 * 
 * @author th
 * 
 */
public class Tray {

	public static void main(String[] args) {
		new Tray();
	}

	public Tray() {
		final TrayIcon trayIcon;

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

}
