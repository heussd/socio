package socio.xmpp;

import org.junit.Before;
import org.junit.Test;

public class XmppClientTest {

	@Before
	public void setUp() throws Exception {
		XmppClient.getInstance();
	}

	@Test
	public void testAddUser() {
		XmppClient.getInstance().addUser("alice-sociodemo@jabber.ccc.de");
	}

	@Test
	public void test() {
//		XmppClient.getInstance().broadcast("Hallo :)");
	}

}
