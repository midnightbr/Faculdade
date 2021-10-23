/**
 * 
 */
/**
 * @author Marcos Henrique
 *
 */
package Principal;

import java.sql.SQLException;

import Model.Bean.Membros;
import Model.DAO.MembrosDAO;

public class TestaFuncionalidade {
	
	public void testaInsercao() throws ClassNotFoundException {
		Membros membrosObj = new Membros();
		MembrosDAO dao = new MembrosDAO();
		membrosObj.setNome("Alex Sander");
		try {
			dao.inserirDados(membrosObj);
			System.out.println("Dado inserido no banco com sucesso!");
		} catch (SQLException e) {
			System.out.println("Problemas na conexão com o banco de dados." + e);
		}
	}
	
	public void testaExclusao() throws ClassNotFoundException {
		Membros membrosObj = new Membros();
		MembrosDAO dao = new MembrosDAO();
		membrosObj.SetId(27);
		try {
			dao.deletarDados(membrosObj);
			System.out.println("Dado removido do banco com sucesso!");
		} catch (SQLException e) {
			System.out.println("Problemas na conexao com o banco de dados. " + e);
		}
	}
	
	public void testaAtualizacao() throws ClassNotFoundException {
		Membros membrosObj = new Membros();
		MembrosDAO dao = new MembrosDAO();
		membrosObj.setNome("Clayton");
		membrosObj.SetId(12);
		try {
			dao.atualizarDados(membrosObj);
			System.out.println("Dado atualizado no banco com sucesso.");
		} catch (SQLException e) {
			System.out.println("Problemas na conexão com o banco de dados. " + e);
		}
	}
	
	public void testSelecao() throws ClassNotFoundException {
		MembrosDAO dao = new MembrosDAO();
		try {
			for (Membros membrosObj : dao.listarDados()) {
				System.out.println("| ID: " + membrosObj.getId() + " | Nome: " + membrosObj.getNome());
			} 
		} catch (SQLException e) {
			System.out.println("Problemas na conexao com o banco de dados. " + e);
		}
	}
	
	// Método principal. Descomenta os métodos que deseja testar.
	public static void main(String[] args) throws ClassNotFoundException {
		TestaFuncionalidade testeFunc = new TestaFuncionalidade();
		
		// Inserir membros, mas altere os valosres no respectivo método acima.
		// testeFunc.testaInsercao();
		
		// Deletar membros, mas altere os valores no respectivo método acima.
		// testeFunc.testaExclusao();
		
		// Atualizar membros, mas altere os valores no respectivo método acima.
		testeFunc.testaAtualizacao();
		
		// Descomente a linha abaixo para listar os membros
		testeFunc.testSelecao();
	}
}