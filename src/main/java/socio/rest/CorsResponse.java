package socio.rest;

import javax.ws.rs.core.Response;

import org.json.JSONObject;

/**
 * This class helps to build special Cross-Origin Resource Sharing (CORS,
 * {@link http://www.w3.org/TR/cors/}) headers into HTTP response. CORS enables
 * AJAX where JS-location != REST-location.
 * 
 * @author th
 * 
 */
public class CorsResponse {

	public static Response ok() {
		return Response.ok().header("Allow-Control-Allow-Methods", "POST,GET,OPTIONS").header("Access-Control-Allow-Origin", "*").build();
	}

	public static Response ok(Object object) {
		return Response.ok().header("Allow-Control-Allow-Methods", "POST,GET,OPTIONS").header("Access-Control-Allow-Origin", "*").entity(object).build();
	}

	/**
	 * Special boolean response, answering true / false in plain text.
	 */
	public static Response ok(Boolean object) {
		return ok(object ? "true" : "false");
	}

	public static Response ok(JSONObject jsonObject) {
		return ok(jsonObject.toString());
	}

	public static Response badRequest() {
		return Response.status(400).header("Allow-Control-Allow-Methods", "POST,GET,OPTIONS").header("Access-Control-Allow-Origin", "*").build();
	}
}
