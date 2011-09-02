package socio.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Consumes(MediaType.APPLICATION_XML)
@Path("/rest")
@Produces(MediaType.TEXT_PLAIN)
public interface SocIoRestApi {

	// Hello Worlds...
	@GET
	@Path("/hello")
	// @Produces("text/plain")
	public Response helloWorld();

	@GET
	@Path("/helloJSON")
	@Produces(MediaType.APPLICATION_JSON)
	public Response helloJSON();

	// ### Productive methods beyond this point ###

	@GET
	@Path("/knows")
	/**
	 * Asks the semantic core if it has knowledge about the given URI.
	 * @return true / false
	 */
	public Response knows(@QueryParam("uri") @DefaultValue("") String uri);

	@GET
	@Path("/add")
	/**
	 * Adds the specified user to the XMPP client. 
	 */
	public Response addXmppUser(@QueryParam("xmpp") @DefaultValue("") String xmpp);

	@GET
	@Path("/queryTag")
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Query tags for containing the given pattern.
	 * @return JSON Array for autocomplete 
	 */
	public Response queryTag(@QueryParam("term") @DefaultValue("") String pattern);

	@GET
	@Path("/queryUri")
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Query tags for a certain resource
	 * @return JSON Array for autocomplete 
	 */
	public Response queryUri(@QueryParam("uri") @DefaultValue("") String uri, @QueryParam("own") @DefaultValue("true") Boolean ownTags);

	@GET
	@Path("/addTag")
	/**
	 * Add tags to a certain resource
	 */
	public Response addTag(@QueryParam("uri") @DefaultValue("") String uri, @QueryParam("tag") @DefaultValue("") String tag);
}