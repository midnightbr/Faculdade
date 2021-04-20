/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ContaCorrente;

import java.util.Scanner;

/**
 *
 * @author Marcos Henrique
 */
class Operator {

    /**
     * @param args the command line arguments
     */
    float saldo;
    
    
    void definirSaldoInicial(float inicial) {
        // Scanner saldoInicial = new Scanner(System.in);
        // float inicial = saldoInicial.nextFloat();
        this.saldo = inicial;
        System.out.format("Saldo atual da conta: R$%.2f \n", this.saldo);
    }
    
    void sacar(float retirada) {
        // Scanner sacar = new Scanner(System.in);
        // retirada = sacar.nextFloat();
        this.saldo = saldo - retirada;
        System.out.format("Saldo atual da conta: R$%.2f \n", this.saldo);
    }
    
    void depositar(float depositar) {
        // Scanner deposito = new Scanner(System.in);
        // depositar = deposito.nextFloat();
        this.saldo = saldo + depositar;
        System.out.format("Saldo atual da conta: R$%.2f \n", this.saldo);
    }
}

public class ContaCorrente {
    public static void main(String[] args) {
        //Scanner teclado = new Scanner(System.in);
        Operator novaConta = new Operator();
        novaConta.definirSaldoInicial(3000);
        System.out.println("Saque aprovado!");
        novaConta.sacar(1000);
        System.out.println("Deposito compensado!");
        novaConta.depositar(500);
        System.out.println("Saque aprovado!");
        novaConta.sacar(600);
    }
}