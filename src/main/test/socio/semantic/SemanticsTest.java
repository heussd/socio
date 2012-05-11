package socio.semantic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import socio.Config;
import socio.model.Promotion;

import com.hp.hpl.jena.rdf.model.Model;

public class SemanticsTest {
	private Semantics semantics;

	@Before
	public void setUp() throws Exception {

		Config.testmode();
		semantics = new Semantics();
	}

	@Test
	public void test() {

		Model model = semantics.constructDemoMessageModel();
		System.out.println(model);

		assertFalse(model.isEmpty());

		model.write(System.out, Semantics.RDF_EXPORT_FORMAT);

		// Try to build a valid import model
		model = semantics.constructValidUserModel(model, Config.getXmppUserId());
		model.write(System.out, Semantics.RDF_EXPORT_FORMAT);
		assertFalse(model.isEmpty());

		// Try to build a INvalid import model (nonmatching user)
		model = semantics.constructValidUserModel(model, "sttde");
		model.write(System.out, Semantics.RDF_EXPORT_FORMAT);
		assertTrue(model.isEmpty());

	}
	
	
	@Test
	public void testExtractActivityEntries() {
		List<Promotion> promotions = semantics.extractActivityEntries(semantics.constructDemoMessageModel());

		Promotion promotion = promotions.get(0);
		System.out.println(promotion.getResource());
	}

}
