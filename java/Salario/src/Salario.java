import java.util.Scanner;

/**
 * 
 */
/**
 * @author Marcos Henrique
 *
 */

public class Salario {

      public static void main(String args[]) {

            Scanner input = new Scanner(System.in);

            double salario, vendas, comissao, total; //Salário, valor da comissão e total
            String name; //Nome do funcionario

            System.out.print("Digite o nome do trabalhador: ");

            name = input.next();

            System.out.print("Digite o salario do trabalhador: ");

            salario = input.nextDouble();

            System.out.print("Digite o total de vendas no mês: ");

            vendas = input.nextDouble();

            comissao = vendas * 0.15;
            

            total = salario + comissao;

            System.out.println("O vendendor " + name + ", tem um salário fixo de R$" + salario + ".");
            System.out.println("Com suas vendas ele ganhou R$" + comissao + " de comissão.");
            System.out.println("No final do mês ele ganhou um total de R$" + total + " reais.");

      }

}
