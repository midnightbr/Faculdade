
/**
 * @author Marcos Henrique
 *
 */
package ts.conversor;

import br.unigoias.ads.ConversorDeNumerosRomanos;
import org.junit.*;
import static org.hamcrest.CoreMatchers.*;

/*
 * Converter número romano para número literal
 * @param numeroRomano
 */
public class ConversorDeNumerosRomanosTest {
	
	@Test
	public void testConverte() {
		ConversorDeNumerosRomanos romano = new ConversorDeNumerosRomanos();
		
		Assert.assertThat(romano.converte("XV"), is(15));
		Assert.assertNotNull(romano.converte("L"));
		Assert.assertThat(romano.converte("M"), equalTo(1000));
		Assert.assertEquals(romano.converte("DC"), 600);
		
	}
}