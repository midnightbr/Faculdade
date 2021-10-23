/**
 * 
 */
/**
 * @author Marcos Henrique
 *
 */
package Principal;

import java.sql.SQLException;

import Funcionarios.Active.Funcionario;
import Funcionarios.DAO.FuncionariosDAO;

public class TestaFuncionalidade {
	
	// Inserir um novo funcionario a empresa
	public void testaInsercao() throws ClassNotFoundException {
		Funcionario funcObj = new Funcionario();
		FuncionariosDAO dao = new FuncionariosDAO();
		// Nome, cargo e salario do novo funcionario
		funcObj.setNome("Marcos Henrique");
		funcObj.setCargo("CEO");
		funcObj.setSalario(25000);
		try {
			// Inserindo os dados passados no BD
			dao.inserirDados(funcObj);
			System.out.println("Dado inserido no BD com sucesso!");
		} catch (SQLException e) {
			System.out.println("Problema na conex�o com o BD. " + e);
		}
	}
	
	// Excluir funcionario do BD
	public void testaExclusao() throws ClassNotFoundException {
		Funcionario funcObj = new Funcionario();
		FuncionariosDAO dao = new FuncionariosDAO();
		// Passando o id que deseja deletar do BD
		funcObj.setId(16);
		try {
			// Deletando o funcionario do BD
			dao.deletarDados(funcObj);
			System.out.println("Dado removido do BD com sucesso!");
		} catch (SQLException e) {
			System.out.println("Problemas na conex�o com o BD. " + e);
		}
	}
	
	// Fun��o para atualizar ou modificar o nome do funcionario
	public void testaAtualizacaoNome() throws ClassNotFoundException {
		Funcionario funcObj = new Funcionario();
		FuncionariosDAO dao = new FuncionariosDAO();
		// Novo nome do funcionario
		funcObj.setNome("Clayton de Souza Pires");
		funcObj.setId(12);
		try {
			// Atualizando o nome dele no BD
			dao.atualizarDadoNome(funcObj);
			System.out.println("Nome atualizado no BD com sucesso!");
		} catch (SQLException e) {
			System.out.println("Problemas na conex�o com o BD. " + e);
		}
	}
	
	// Fun��o para alterar o salario do funcionario
	public void testaAtualizacaoSalario() throws ClassNotFoundException {
		Funcionario funcObj = new Funcionario();
		FuncionariosDAO dao = new FuncionariosDAO();
		funcObj.setSalario(2000);
		funcObj.setId(12);
		try {
			dao.atualizarDadoSalario(funcObj);
			System.out.println("Salario atualizado no BD com sucesso!");
		} catch (SQLException e) {
			System.out.println("Problemas na conex�o com o BD. " + e);
		}
	}
	
	// Func�o para alterar o cargo do funcionario
	public void testaAtualizacaoCargo() throws ClassNotFoundException {
		Funcionario funcObj = new Funcionario();
		FuncionariosDAO dao = new FuncionariosDAO();
		funcObj.setCargo("Suporte");
		funcObj.setId(12);
		try {
			dao.atualizarDadoCargo(funcObj);
			System.out.println("Cargo atualizado no BD com sucesso!");
		} catch (SQLException e) {
			System.out.println("Problemas na conex�o com o BD. " + e);
		}
	}
	
	// Fun��o para atualizar todos os dados de uma s� vez
	public void testaAtualizacao() throws ClassNotFoundException {
		Funcionario funcObj = new Funcionario();
		FuncionariosDAO dao = new FuncionariosDAO();
		// Novo nome do funcionario
		funcObj.setNome("Clayton de Souza");
		funcObj.setCargo("T�cnico em informatica");
		funcObj.setSalario(1800);
		funcObj.setId(12);
		try {
			// Atualizando todos os dados dele no BD
			dao.atualizarDado(funcObj);
			System.out.println("Nome atualizado no BD com sucesso!");
		} catch (SQLException e) {
			System.out.println("Problemas na conex�o com o BD. " + e);
		}
	}
	
	// Classe para vizualiza��o dos itens no BD
	public void testSelecao() throws ClassNotFoundException {
		FuncionariosDAO dao = new FuncionariosDAO();
		try {
			for (Funcionario funcObj : dao.listarDados()) {
				System.out.println("| ID: " + funcObj.getId() + " | Nome: " + funcObj.getNome() + 
						" | Salario: " + funcObj.getSalario() + " | Cargo: " + funcObj.getCargo() + " |");
			}
		} catch (SQLException e) {
			System.out.println("Problemas na conex�o com o BD. " + e);
		}
	}
	
	public static void main(String[] args) throws ClassNotFoundException {
		TestaFuncionalidade testFunc = new TestaFuncionalidade();
		
		// Adicionado um item no BD
		// testFunc.testaInsercao();
		
		// Exclui um objeto do BD
		// testFunc.testaExclusao();
		
		// Altera��o de um algum objeto no BD
		// Altera o nome
		//testFunc.testaAtualizacaoNome();
		
		// Altera o salario do funcionario
		// testFunc.testaAtualizacaoSalario();
		
		// Altera o cargo do funcionario
		// testFunc.testaAtualizacaoCargo();
		
		// Atualizando todos os dados do funcionario
		// testFunc.testaAtualizacao();
		
		// Comando usado para visualizar os arquivos do BD
		testFunc.testSelecao();
	}
}