package socio.semantic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;

public class SemanticsTest {
	private Semantics semantics;

	@Before
	public void setUp() throws Exception {
		semantics = new Semantics();
	}

	@Test
	public void test() {

		Model model = semantics.constructDemoMessageModel();
		System.out.println(model);

		assertFalse(model.isEmpty());

		model.write(System.out, Semantics.RDF_EXPORT_FORMAT);

		// Try to build a valid import model
		model = semantics.constructValidModel(model, "xmpp://socio@example.com");
		model.write(System.out, Semantics.RDF_EXPORT_FORMAT);
		assertFalse(model.isEmpty());

		// Try to build a INvalid import model (nonmatching user)
		model = semantics.constructValidModel(model, "sttde");
		model.write(System.out, Semantics.RDF_EXPORT_FORMAT);
		assertTrue(model.isEmpty());

	}

}
