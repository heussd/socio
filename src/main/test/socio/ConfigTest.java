package socio;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ConfigTest {

	Config config;

	@Before
	public void setUp() throws Exception {
		config = Config.getInstance();
	}

	@Test
	public void testValidation() {
		assertTrue(config.isValidXmppId("xmpp://validuser@domain.com"));
		assertFalse(config.isValidXmppId("xmpp://user"));
		assertFalse(config.isValidXmppId("user"));
		assertFalse(config.isValidXmppId("user@domain"));
		assertFalse(config.isValidXmppId(null));
		assertFalse(config.isValidXmppId(""));
	}

}
