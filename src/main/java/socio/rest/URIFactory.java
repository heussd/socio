package socio.rest;

import java.net.URI;

/**
 * This class converts strings into URIs. The special thing is: It checks if the
 * query part of the uri is valid.
 * 
 * @author th
 * 
 */
public class URIFactory {
	// private static String ENCODING = "UTF-8";

	public static URI getUri(String uriString) throws Exception {
		URI uri = new URI(uriString.replaceAll(" ", "+"));

		// These lines make me sad :(
		if (uri.getQuery() != null) {
			// Make sure the query part does not contain invalid brackets
			if (!uri.getQuery().replaceAll("\\[", "").replaceAll("\\]", "").equals(uri.getQuery())) {
				uriString = uri.toString().replaceAll("\\[", "%5B").replaceAll("\\]", "%5D");
				uri = new URI(uriString);
			}
		}

		return uri;
	}

	public static URI getUriSilent(String uriString) {
		try {
			return getUri(uriString);
		} catch (Exception e) {
		}
		return null;
	}

}
