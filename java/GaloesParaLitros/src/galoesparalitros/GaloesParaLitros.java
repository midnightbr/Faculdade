/*
 * Tente isto 1-1
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
        double gallons; //Contém o número de galões
        double liters; //Contpem a conversão para litros
        
        gallons = 10; //Começa com 10 galões
        
        liters = gallons * 3.7854; //Convertendo para litros
        
        System.out.println(gallons + " gallons is " + liters + "liters.");
    }
    
}
