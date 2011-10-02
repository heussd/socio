package socio;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ConfigTest {

	@Before
	public void setUp() throws Exception {
		Config.testmode();
	}

	@Test
	public void testValidation() {
		assertTrue(Config.isValidXmppId("xmpp://validuser@domain.com"));
		assertFalse(Config.isValidXmppId("xmpp://user"));
		assertFalse(Config.isValidXmppId("user"));
		assertFalse(Config.isValidXmppId("user@domain"));
		assertFalse(Config.isValidXmppId(null));
		assertFalse(Config.isValidXmppId(""));
	}

}
