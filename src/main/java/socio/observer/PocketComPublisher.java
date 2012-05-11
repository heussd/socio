package socio.observer;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import org.apache.log4j.Logger;

import socio.Config;
import socio.model.Promotion;
import socio.semantic.SemanticCore;

public class PocketComPublisher implements Observer {

	private final static String URL_ENCODING = "UTF-8";
	private final static Logger LOGGER = Logger.getLogger(PocketComPublisher.class);

	private final static String POCKET_BASE_URI = "https://readitlaterlist.com/v2/add";
	private final static String POCKET_API_KEY = Config.getPocketApiKey();

	private String base_url;

	public PocketComPublisher() throws Exception {
		setup(Config.getPocketUsername(), Config.getPocketPassword());
	}

	public PocketComPublisher(String user, String pass) throws Exception {
		setup(user, pass);
	}

	private void setup(String user, String pass) throws Exception {
		LOGGER.info("Bringing up Pocket.com publisher...");
		if (user.equals("") | pass.equals(""))
			throw new RuntimeException("Could not bring up PocketComPublisher: Missing username or password.");

		base_url = new String(POCKET_BASE_URI + "?username=" + URLEncoder.encode(user, URL_ENCODING) + "&password=" + URLEncoder.encode(pass, URL_ENCODING) + "&apikey="
				+ POCKET_API_KEY);
	}

	@Override
	public void update(Observable o, Object arg) {
		if (o instanceof SemanticCore) {
			@SuppressWarnings("unchecked")
			ArrayList<Promotion> promotions = (ArrayList<Promotion>) arg;
			try {
				for (Promotion promotion : promotions) {

					if (!promotion.getUser().equals(Config.getUserName())) {

						LOGGER.debug("Publishing promotion " + promotion);

						URL url = new URL(base_url + "&url=" + URLEncoder.encode(promotion.getResource(), URL_ENCODING) + "&title="
								+ URLEncoder.encode(promotion.getUser(), URL_ENCODING));
						HttpURLConnection connection = (HttpURLConnection) url.openConnection();

						LOGGER.debug("Fired Pocket query, received HTTP code " + connection.getResponseCode());
					}
					LOGGER.debug("Will not publish own promotion to Pocket");
				}
			} catch (Exception e) {
				LOGGER.error("Could not fire Pocket query", e);
			}
		}

	}
}
