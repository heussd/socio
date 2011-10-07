package socio.rest;

import java.net.URI;
import java.net.URLEncoder;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import socio.rss.ActivityFeed;
import socio.semantic.SemanticCore;
import socio.xmpp.XmppClient;

public class RestApi implements SocIoRestApi {
	private static final String URI_ENCODING = "UTF-8";
	private static Logger logger = Logger.getLogger(RestApi.class);

	@GET
	@Produces("text/plain")
	public String echo(@QueryParam("text") String text) {
		System.out.println(text);
		return text;
	}

	@Override
	/**
	 * http://localhost:8080/socio/rest/hello
	 */
	public Response helloWorld() {
		logger.info("Received hello, replying hello...");
		return CorsResponse.ok("Hello World!");

	}

	@Override
	public Response helloJSON() {
		try {
			JSONObject jsonArray = new JSONObject("{ 'age' :25, 'address': { 'streetAddress': '21 2nd Street', 'city': 'New York' } }");
			return CorsResponse.ok(jsonArray.toString());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		return CorsResponse.badRequest();

	}

	@Override
	public Response knows(String uri) {
		try {
			uri = URLEncoder.encode(uri, URI_ENCODING);
			logger.debug("Received ask for knowledge on:" + uri);
			logger.debug("Querying semantic core...");
			uri = URLEncoder.encode(uri, URI_ENCODING);
			return CorsResponse.ok(SemanticCore.getInstance().classifyKnowledgeAbout(new URI(uri)));

		} catch (Exception e) {
			logger.error("Could not query for knowledge on " + uri, e);
		}
		return CorsResponse.badRequest();

	}

	@Override
	public Response addXmppUser(String xmpp) {
		logger.debug("Received add user for " + xmpp);
		return CorsResponse.ok(XmppClient.getInstance().addUser(xmpp));
	}

	@Override
	public Response queryTag(String pattern) {
		logger.debug("Tag query for pattern " + pattern);
		return CorsResponse.ok(new JSONArray(SemanticCore.getInstance().queryTagsForPattern(pattern)).toString());
	}

	@Override
	public Response queryUri(String uri, Boolean ownTags) {
		try {
			uri = URLEncoder.encode(uri, URI_ENCODING);
			logger.debug("Tag query for uri " + uri + ", own tags = " + ownTags);
			return CorsResponse.ok(new JSONArray(SemanticCore.getInstance().queryTagsForUri(uri, ownTags)).toString());
		} catch (Exception e) {
			logger.error("Could not query URI " + uri, e);
		}
		return CorsResponse.badRequest();
	}

	@Override
	public Response addTag(String uri, String tag) {
		// Remove tag / url delimiters (if necessary)
		// TODO: Is this still needed?
		tag = tag.replaceAll("'", "").replaceAll("\"", "");
		uri = uri.replaceAll("'", "").replaceAll("\"", "");

		try {
			uri = URLEncoder.encode(uri, URI_ENCODING);

			if (!tag.equals("")) {
				logger.debug("Add tag " + tag + " to resource " + uri);
				SemanticCore.getInstance().addTags(uri, new String[] { tag });
			} else {
				logger.debug("Tag was empty, will not add.");
			}
			return CorsResponse.ok();
		} catch (Exception e) {
			logger.error("Could not add tag + " + tag + " to uri " + uri, e);
		}
		return CorsResponse.badRequest();
	}

	@Override
	public Response queryRelated(String uri, Boolean ownFlag) {
		try {
			uri = URLEncoder.encode(uri, URI_ENCODING);
			return CorsResponse.ok(new JSONObject(SemanticCore.getInstance().queryRelatedUris(new URI(uri), ownFlag)));
		} catch (Exception e) {
			logger.error("Could not query related uris:", e);
		}
		return CorsResponse.badRequest();
	}

	@Override
	public Response activity(String tag) {
		// Remove tag delimiters (if necessary)
		tag = tag.replaceAll("'", "").replace("\"", "");

		ActivityFeed activityFeed = new ActivityFeed(tag);
		activityFeed.addEntries(SemanticCore.getInstance().queryTagActivity(tag));
		return CorsResponse.ok(activityFeed.toString());
	}
}
