/*
 * Tente isto 1-2
 * Este programa converte galões em litros
 */
package galoesparalitros;

/**
 *
 * @author Marcos Henrique
 */
public class GaloesParaLitros {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        double gallons, liters;
        int counter;
        
        counter = 0;
        for(gallons = 1; gallons <= 100; gallons++) {
            liters = gallons * 3.7854; // Convertendo para litros
            System.out.println(gallons + " gallons is " + liters + " liters.");
            
            counter++;
            // A cada décima linha, exibe uma linha em branco
            if(counter == 10) {
                System.out.println();
                counter = 0; // Zera o contador de linhas
            }
        }
    }
    
}
