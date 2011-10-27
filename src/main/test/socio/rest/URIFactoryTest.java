package socio.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.net.URI;

import org.junit.Test;

public class URIFactoryTest {

	@Test
	public void test() throws Exception {

		URI uri = new URI("http://www.google.de");

		assertEquals(uri, URIFactory.getUri(uri.toString()));
		
		uri = new URI("http://soundcloud.com/search?q[fulltext]=balkan");
		
		assertFalse(uri.equals(URIFactory.getUri(uri.toString())));
		
	}

}
