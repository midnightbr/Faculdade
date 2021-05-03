/*
 * Demonstra um bloco de código
 */
package blocodemo;

/**
 *
 * @author marco
 */
public class BlocoDemo {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        double i, j, d;
        
        i = 5;
        j = 10;
        
        // O alvo desta intrução if é o bloco código
        if(i != 0) {
            System.out.println("I does not equal zero");
            d = j / i;
            System.out.println("J / I is " + d);
        }
    }
    
}
