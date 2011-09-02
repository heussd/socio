package socio.semantic;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import socio.Config;

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
		model = semantics.constructValidModel(model, Config.getInstance().getXmppUserId());
		model.write(System.out, Semantics.RDF_EXPORT_FORMAT);
		assertFalse(model.isEmpty());
//
//		// Try to build a INvalid import model (nonmatching user)
//		model = semantics.constructImportGraph(model, "sttde");
//		model.write(System.out, Semantics.RDF_EXPORT_FORMAT);
//		assertTrue(model.isEmpty());

	}

}
