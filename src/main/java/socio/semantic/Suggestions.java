package socio.semantic;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class Suggestions {

	private static final Logger LOGGER = Logger.getLogger(Suggestions.class);

	private LinkedHashMap<String, String> suggestions;

	public Suggestions(String input) {
		suggestions = new LinkedHashMap<String, String>();
	}

	public void put(String url, String label, String score) {
		// TODO: score is currently not used.
		suggestions.put(url, label);
		LOGGER.debug("Added new suggestion: " + url + " (" + label + ", " + score + ")");
	}

	public int size() {
		return suggestions.size();
	}

	/**
	 * This method converts the suggestions into Chrome's Omnibar JSON.
	 */
	public String toJson() {
		JSONArray root = new JSONArray();

		for (Map.Entry<String, String> suggestion : suggestions.entrySet()) {
			try {
				root.put(new JSONObject("{content: \"" + suggestion.getKey() + "\", description: \""
						+ (suggestion.getValue().equals("") ? suggestion.getKey() : suggestion.getValue()) + "\"}\n"));
			} catch (Exception e) {
				LOGGER.error("Could not convert suggestion to JSON", e);
			}
		}
		return root.toString();
	}
}
