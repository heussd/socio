package socio.tray;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.apache.log4j.Logger;

public class UriBrowserActionListener implements ActionListener {

	private final static Logger LOGGER = Logger.getLogger(UriBrowserActionListener.class);
	private String uriString;

	public UriBrowserActionListener(String uri) {
		this.uriString = uri;
	}

	@Override
	public void actionPerformed(ActionEvent actionEvent) {
		if (java.awt.Desktop.isDesktopSupported()) {
			java.awt.Desktop desktop = java.awt.Desktop.getDesktop();

			if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
				try {
					java.net.URI uri = new java.net.URI(uriString);
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

}
