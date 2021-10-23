/**
 * 
 */
/**
 * @author Marcos Henrique
 *
 */
package Model.DAO;

import DatabaseConnection.Conexao;
import Model.Bean.Membros;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MembrosDAO {
	
	public void inserirDados(Membros membro) throws SQLException, ClassNotFoundException {
		// Conectando ao BD
		Connection con = Conexao.conector();
		// Apontamento SQL pré-compilado
		PreparedStatement prepStmt = null;
		try {
			prepStmt = con.prepareStatement("INSERT INTO membros VALUES (DEFAULT, ?)");
			prepStmt.setString(1, membro.getNome());
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
	
	public void deletarDados(Membros m) throws SQLException, ClassNotFoundException {
		// Conectando ao BD
		Connection con = Conexao.conector();
		// Apontamento SQL pré-compilado
		PreparedStatement stmt = null;
		try {
			stmt = con.prepareStatement("DELETE FROM membros WHERE id = ?");
			
			stmt.setInt(1, m.getId());
			stmt.executeUpdate();
			
		} catch (SQLException ex) {
			System.out.println(ex);
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ignore) {
					
				}
			if (con != null) {
				try {
					con.close();
				} catch (SQLException ignore) {
					
				}
			}
		}
	}
	
	public void atualizarDados(Membros m) throws SQLException, ClassNotFoundException {
		// Conectando ao BD
		Connection con = Conexao.conector();
		// Apontamento SQL pré-compilado
		PreparedStatement stmt = null;
		try {
			stmt = con.prepareStatement("UPDATE membros SET nome = ? WHERE id = ?");
			stmt.setString(1, m.getNome());
			stmt.setInt(2, m.getId());
			stmt.executeUpdate();
		} catch (SQLException ex) {
			System.out.println(ex);
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ignore) {
					
				}
			if (con != null)
				try {
					con.close();
				} catch (SQLException ignore) {
					
				}
		}
	}
	
	public List<Membros> listarDados() throws SQLException, ClassNotFoundException {
		// Conectando ao BD
		Connection con = Conexao.conector();
		// Apontamento SQL pré-compilado
		PreparedStatement stmt = null;
		// Armazena o resultado de uma busca
		ResultSet rs = null;
		
		List<Membros> listaDeMembros = new ArrayList<>();
		try {
			stmt = con.prepareStatement("SELECT * FROM membros");
			rs = stmt.executeQuery();
			while (rs.next()) {
				Membros membroObj = new Membros();
				membroObj.SetId(rs.getInt("id"));
				membroObj.setNome(rs.getString("nome"));
				listaDeMembros.add(membroObj);
			}
		} catch (SQLException ex) {
			System.out.println(ex);
		} finally {
			if (rs != null)
				try {
					rs.close();
				} catch (SQLException ignore) {
					
				}
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ignore) {
					
				}
			if (con != null)
				try {
					con.close();
				} catch (SQLException ignore) {
					
				}
		}
		
		return listaDeMembros;
	}
}