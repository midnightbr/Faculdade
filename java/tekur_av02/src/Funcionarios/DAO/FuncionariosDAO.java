/**
 * 
 */
/**
 * @author Marcos Henrique
 *
 */
package Funcionarios.DAO;

import DatabaseConnection.Conexao;
import Funcionarios.Active.Funcionario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FuncionariosDAO {
	
	public void inserirDados(Funcionario funcionario) throws SQLException, ClassNotFoundException {
		// Conectando ao BD
		Connection bd = Conexao.conector();
		// Apontamento SQL pré-compilado
		PreparedStatement prepStmt = null;
		try {
			prepStmt = bd.prepareStatement("INSERT INTO funcionarios VALUES (DEFAULT, ?, ?, ?)");
			prepStmt.setString(1, funcionario.getNome());
			prepStmt.setInt(2, funcionario.getSalario());
			prepStmt.setString(3, funcionario.getCargo());
			prepStmt.executeUpdate();
		} catch (SQLException ex) {
			System.out.println(ex);
		} finally {
			if (prepStmt != null)
				try {
					prepStmt.close();
				} catch (SQLException ignore) {
					
				}
		}
	}
	
	public void deletarDados (Funcionario f) throws SQLException, ClassNotFoundException {
		// Conectando ao BD
		Connection bd = Conexao.conector();
		
		PreparedStatement stmt = null;
		try {
			stmt = bd.prepareStatement("DELETE FROM funcionarios WHERE id = ?");
			
			stmt.setInt(1, f.getId());
			stmt.executeUpdate();
		} catch (SQLException ex) {
			System.out.println(ex);
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ignore) {
					
				}
			
			if (bd != null)
				try {
					bd.close();
				} catch (SQLException ignore) {
					
				}
		}
	}
	
	public void atualizarDadoNome(Funcionario f) throws SQLException, ClassNotFoundException {
		Connection bd = Conexao.conector();
		PreparedStatement stmt = null;
		
		try {
			stmt = bd.prepareStatement("UPDATE funcionarios SET nome = ? WHERE id = ?");
			stmt.setString(1, f.getNome());
			stmt.setInt(2, f.getId());
			stmt.executeUpdate();
		} catch (SQLException ex) {
			System.out.println(ex);
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ignore) {
					
				}
			
			if (bd != null)
				try {
					bd.close();
				} catch (SQLException ignore) {
					
				}
		}
	}
	
	public void atualizarDadoSalario(Funcionario f) throws SQLException, ClassNotFoundException {
		Connection bd = Conexao.conector();
		PreparedStatement stmt = null;
		
		try {
			stmt = bd.prepareStatement("UPDATE funcionarios SET salario = ? WHERE id = ?");
			stmt.setInt(1, f.getSalario());
			stmt.setInt(2, f.getId());
			stmt.executeUpdate();
		} catch (SQLException ex) {
			System.out.println(ex);
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ignore) {
					
				}
			
			if (bd != null)
				try {
					bd.close();
				} catch (SQLException ignore) {
					
				}
		}
	}
	
	public void atualizarDadoCargo(Funcionario f) throws SQLException, ClassNotFoundException {
		Connection bd = Conexao.conector();
		PreparedStatement stmt = null;
		
		try {
			stmt = bd.prepareStatement("UPDATE funcionarios SET cargo = ? WHERE id = ?");
			stmt.setString(1, f.getCargo());
			stmt.setInt(2, f.getId());
			stmt.executeUpdate();
		} catch (SQLException ex) {
			System.out.println(ex);
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ignore) {
					
				}
			
			if (bd != null)
				try {
					bd.close();
				} catch (SQLException ignore) {
					
				}
		}
	}
	
	public void atualizarDado(Funcionario f) throws SQLException, ClassNotFoundException {
		Connection bd = Conexao.conector();
		PreparedStatement stmt = null;
		
		try {
			stmt = bd.prepareStatement("UPDATE funcionarios SET nome = ?, salario = ?, cargo = ? WHERE id = ?");
			stmt.setString(1, f.getNome());
			stmt.setInt(2, f.getSalario());
			stmt.setString(3, f.getCargo());
			stmt.setInt(4, f.getId());
			stmt.executeUpdate();
		} catch (SQLException ex) {
			System.out.println(ex);
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ignore) {
					
				}
			
			if (bd != null)
				try {
					bd.close();
				} catch (SQLException ignore) {
					
				}
		}
	}
	
	public List<Funcionario> listarDados() throws SQLException, ClassNotFoundException {
		Connection bd = Conexao.conector();
		PreparedStatement stmt = null;
		ResultSet res = null;
		
		List<Funcionario> listaDeFuncionarios = new ArrayList<>();
		try {
			stmt = bd.prepareStatement("SELECT * FROM funcionarios");
			res = stmt.executeQuery();
			
			while (res.next()) {
				Funcionario funObj = new Funcionario();
				funObj.setId(res.getInt("id"));
				funObj.setNome(res.getString("nome"));
				funObj.setSalario(res.getInt("salario"));
				funObj.setCargo(res.getString("cargo"));
				listaDeFuncionarios.add(funObj);
			}
		} catch (SQLException ex) {
			System.out.println(ex);
		} finally {
			if (res != null)
				try {
					res.close();
				} catch (SQLException ignore) {
					
				}
			
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ignore) {
					
				}
			
			if (bd != null) 
				try {
					bd.close();
				} catch (SQLException ignore) {
					
				}
		}
		
		return listaDeFuncionarios;
	}
	
}

