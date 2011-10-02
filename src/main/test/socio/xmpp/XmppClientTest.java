package socio.xmpp;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import socio.Config;
import socio.semantic.SemanticCore;
import socio.semantic.Semantics;

@Ignore("This class must be executed manually!")
public class XmppClientTest {

	private Semantics semantics;
	private Boolean has_junit_config;

	@Before
	public void setUp() throws Exception {

		// This test requires working Jabber credentials, which are naturally
		// not available on the public repository. However, the test should NOT
		// fail because of a missing unit config file.
		has_junit_config = Config.class.getClassLoader().getResourceAsStream("junit.properties") != null ? true : false;

		Config.testmode();
		semantics = new Semantics();
	}

	@Test
	public void testAddUser() throws Exception {
		if (has_junit_config) {
			SemanticCore core = SemanticCore.getInstance();
			core.clear();
			core.persistStatements(semantics.constructDemoMessageModel());
			core.addTags("http://example.com", "Sample");

			// Expected: two messages
			XmppClient.getInstance().addUser("xmpp://alice-sociodemo@jabber.ccc.de");
		}
	}
}