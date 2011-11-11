package socio.semantic;

import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

public class Suggestions {

	private static final Logger LOGGER = Logger.getLogger(Suggestions.class);

	private LinkedHashMap<String, String> suggestions;

	public Suggestions(String input) {
		suggestions = new LinkedHashMap<String, String>();
	}

	public void put(String url, String label, String score) {
		// TODO: score is currently not used.
		suggestions.put(url, label);
		LOGGER.debug("Added new suggestion: " + url + " (" + label + ")");
	}

	public int size() {
		return suggestions.size();
	}
}
