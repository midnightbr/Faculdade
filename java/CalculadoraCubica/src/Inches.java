/**
 * @author Marcos Henrique
 *
 * Calcula quantas polegadas cúbicas há
 * em uma milha cúbica.
 */
class Inches {
	// Usando o tipo inteiro long.
	public static void main(String args[]) {
		long ci;
		long im;
		
		im = 5280 * 12;
		
		ci = im * im * im;
		
		System.out.println("There are " + ci + " cubic inches in cubic mile.");
	}
}