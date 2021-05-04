/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pensemais;

import java.util.Scanner;

/**
 *
 * @author Marcos Henrique
 */

class Funcionario {
    String nome, cpf;
    double salario;
    
    // Constructor para classe Funcionario
    Funcionario(String nome, String cpf, double salario) {
        this.nome = nome;
        this.cpf = cpf;
        this.salario = salario;
    }
    
    public String get_nome() {
        return this.nome;
    }
    
    public String get_cpf() {
        return this.cpf;
    }
    
    double get_salario() {
        return this.salario;
    }
    
    public void setNome(String nome) {
        this.nome = nome;
    }
    
    public void setCpf(String cpf) {
        this.cpf = cpf;
    }
    
    public void setSalario(double salario) {
        this.salario = salario;
    }
}

class Gerente extends Funcionario {
    int senha;
    
    public Gerente(String nome, String cpf, double salario, int senha) {
        super(nome, cpf, salario);
        this.senha = senha;
        
    }
    
    boolean autentica(int login) {
        //int login;
        int senha;
        senha = this.senha;
        boolean acess;
        
        if (senha == login) {
            acess = true;
        } else {
            acess = false;
        }
        
        return acess;
    }
    
}

public class PenseMais {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        Scanner pessoa = new Scanner(System.in);
        String name, cpf;
        double salario;
        int senha = 0;
        
//        System.out.print("Nome da pessoa: ");
//        name = pessoa.nextLine();
//        System.out.print("Digite o CPF:");
//        cpf = pessoa.nextLine();
//        System.out.print("Qual o salario? ");
//        salario = pessoa.nextDouble();
        name = "Carlos";
        cpf = "123456789";
        salario = 2500;
        Funcionario func = new Funcionario(name, cpf, salario);
        
//        System.out.print("Nome da pessoa: ");
//        name = pessoa.nextLine();
//        System.out.print("Digite o CPF:");
//        cpf = pessoa.nextLine();
//        System.out.print("Qual o salario? ");
//        salario = pessoa.nextDouble();
//        System.out.print("Digite uma senha: ");
//        senha = pessoa.nextInt();
        name = "Idelbrando";
        cpf = "987654321";
        salario = 4200;
        senha = 12342;
        Gerente geren = new Gerente(name, cpf, salario, senha);
        
        System.out.println(func.get_nome());
        System.out.println(func.get_cpf());
        System.out.println(Double.toString(func.get_salario()));
        
        System.out.println(geren.get_nome());
        System.out.println(geren.get_cpf());
        System.out.println(Double.toString(geren.get_salario()));
        
        System.out.println(geren.autentica(102030));
    }
}
