/*
 * Demonstra a instrução if
 */
package condicaoif;

/**
 *
 * @author Marcos Henrique
 */
public class CondicaoIf {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        int a, b, c;
        
        a = 2;
        b = 3;
        
        if(a < b) System.out.println("A is less than B");
        // Está intrução não exibirá nada
        if (a == b) System.out.println("You won't see this");
        
        System.out.println();
        
        c = a - b; // c contém -1
        
        System.out.println("C countais -1");
        if(c >= 0) System.out.println("C is non-negative");
        if(c < 0) System.out.println("C is negative");
        
        System.out.println();
        
        c = b - a; // Agora c contém 1
        
        System.out.println("C countais 1");
        if(c >= 0) System.out.println("C is non-negative");
        if(c < 0) System.out.println("C is negative");
    }
    
}
