package socio.rest;

import java.net.URI;
import java.net.URLEncoder;

/**
 * This class converts strings into URIs. The special thing is: It checks if the
 * query part of the uri is valid.
 * 
 * @author th
 * 
 */
public class URIFactory {
	private static String ENCODING = "UTF-8";

	public static URI getUri(String uriString) throws Exception {
		URI uri = new URI(uriString);

		if (uri.getQuery() != null) {
			// Make sure the query part does not contain invalid brackets
			if (!uri.getQuery().replaceAll("\\[", "").replaceAll("\\]", "").equals(uri.getQuery())) {
				String query = URLEncoder.encode(uri.getQuery(), ENCODING);
				uri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), query, uri.getFragment());
			}
		}

		return uri;
	}
}
