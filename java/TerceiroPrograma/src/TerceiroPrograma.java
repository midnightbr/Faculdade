/**
  	Esse programa ilustra a diferen�a
  	entre int e double.
 */
/**
 * @author Marcos Henrique
 *
 */
class TerceiroPrograma {
	public static void main(String args[]) {
		int var; //essa instru��o declara uma vari�vel int
		double x; //essa instru��o declara uma vari�vel de ponto flutuante
		
		var = 10; //atribuindo a var o valor 10;
		
		x = 10.0; //atribuindo a x o valor 10.0
		
		System.out.println("Original value of var: " + var);
		System.out.println("Original value of x: " + x);
		System.out.println(); //exibe uma linha em branco
		
		//agora divide as duas por 4
		var = var / 4;
		x = x / 4;
		
		System.out.println("var after division: " + var);
		System.out.println("x after division: " + x);
	}
}