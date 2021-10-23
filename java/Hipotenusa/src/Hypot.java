/**
 * Usa o teorema de Pitágoras para encontrar o comprimento da
 * hipotenusa dados os comprimentos dos dois lados apostos
 */
/**
 * @author Marcos Henrique
 *
 */
class Hypot {
	public static void main(String args[]) {
		double x, y, z;
		
		// Passando valores para calcular a hypotenusa
		x = 3;
		y = 4;
		
		// Calculando a hypotenusa usando a biblioteca math(sqrt)
		z = Math.sqrt(x*x + y*y);
		
		System.out.println("Hypotenuse is " + z);
	}
}