/**
 * 
 */
/**
 * @author Marcos Henrique
 *
 */
package teste;

import org.junit.Assert;
import prova.Calculator;
import org.junit.Test;

public class CalculatorTest {
	
	@Test
	static public void assertEquals(double expected, double actual, double delta) {
		Calculator calculator = new Calculator();
		
		calculator.add(expected, actual);
	}
}