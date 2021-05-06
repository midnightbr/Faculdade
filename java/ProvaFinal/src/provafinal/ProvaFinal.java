/*
 * Agora, crie uma classe denominada Lampada que obedeça a descrição apresentada na representação abaixo.

 * A classe deve: 

 * 1) Ter os atributos acesa e potencia. E os métodos: acender, apagar, informarSituacao e informarPotencia.

 * O método acender deve alterar o atributo acesa para true;
 * O método apagar deve alterar o atributo acesa para false;
 * O método informarSituacao deve informar a mensagem “A luz está acesa” caso o atributo acesa seja igual a true e a mensagem “A luz está apagada” caso o atributo acesa seja igual a false;
 * O método informarPotencia deve escrever a mensagem “A potência da lâmpara é X”, onde X é o valor do atribuito potência;
 * 2) Crie também um método main que realize as seguintes operações:

 * Instancie um objeto do tipo Lampada.
 * Chame o método acender.
 * Chame o método informarSituacao.
 * Chame o método apagar
 * Chame o método informarSituacao
 * Chame o método informarPotencia
 */
package provafinal;

/**
 *
 * @author Marcos Henrique
 */
class Lampada {
    boolean acesa;
    double potencia;
    
    void acender() {
        boolean lampada = this.acesa;
        if(lampada == false) {
            this.acesa = true;
        }
    }
    
    void apagar() {
        boolean lampada = this.acesa;
        if(lampada == true) {
            this.acesa = false;
        }
    }
    
    void informarSituacao() {
        if(this.acesa == true) {
            System.out.println("A luz está acesa!");
        } else {
            System.out.println("A luz está apagada!");
        }   
    }
    
    void informarPotencia(double watts) {
        this.potencia = watts;
        System.out.println("A potência da lâmpada é de " + watts + " watts");
    }
    
}

public class ProvaFinal {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        Lampada lamp = new Lampada();
        lamp.acender();
        lamp.informarSituacao();
        
        lamp.apagar();
        lamp.informarSituacao();
        
        lamp.informarPotencia(5);
    }
    
}
