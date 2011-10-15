package socio.rest;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.log4j.Logger;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class RestLauncher {
	private static Logger logger = Logger.getLogger(RestLauncher.class);

	private HttpServer httpServer;
	private HttpContext httpContext;

	public void bringUpRestApi(int port) throws Exception {
		try {
			logger.info("Bringing up RESTful API...");

			httpServer = HttpServer.create(new InetSocketAddress("localhost", port), 25);
			httpContext = httpServer.createContext("/socio");

			HttpHandler httpHandler = RuntimeDelegate.getInstance().createEndpoint(new Application() {

				@Override
				public Set<Class<?>> getClasses() {
					Set<Class<?>> classes = new HashSet<Class<?>>();

					classes.add(RestApi.class);
					return classes;
				}
			}, HttpHandler.class);
			httpContext.setHandler(httpHandler);

			httpServer.start();
			logger.info("RESTful API is up.");
		} catch (Exception e) {
			throw new Exception("Could not bring up RESTful API:", e);
		}
	}
}
