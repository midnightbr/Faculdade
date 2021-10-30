/**
 * 
 */
/**
 * @author marcos
 *
 */
package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import database.ConnectionFactory;
import model.Raca;

public class RacaDAO {
	
	private Connection connection;
	
	public RacaDAO() {
		this.connection = new ConnectionFactory().getConnection();
	}
	
	public Raca getById(int id) {
		String sql = "SELECT RAC.RAC_IDEN, RAC_NOME " + "FROM RACAS RAC " + "WHERE RAC.RAC_IDEN = " + id;
		
		Raca raca = new Raca();
		
		try {
			PreparedStatement stm = connection.prepareStatement(sql);
			
			ResultSet rs = stm.executeQuery();
			
			while (rs.next()) {
				raca.setId(rs.getInt("RAC_IDEN"));
				raca.setNome(rs.getString("RAC_NOME"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			raca = null;
		}
		
		return raca;
	}

	public void inserirRaca(Raca raca) {
		
		// Raca raca = new Raca();
		// String res;
		
		try {
//			String sql = "INSERT INTO raca VALUES (DEFAULT, ?)";
			PreparedStatement stm = connection.prepareStatement("INSERT INTO racas VALUES (?, ?)");
//			stm.setInt(1, raca.getId());
//			stm.setString(2, raca.getNome());
//			int raca_id = Integer.parseInt(id_raca);
			stm.setInt(1, raca.getId());
			stm.setString(2, raca.getNome());
			stm.executeUpdate();
			
			// res = "Raça cadastada com sucesso";
		} catch (SQLException e) {
			e.printStackTrace();
			// res = "Não foi possivel cadastrar a raça. Erro: " + e;
			
		}
		
		// return res;
	}
	
	public String alterarRaca(String id_raca, String nome_raca) throws SQLException, ClassNotFoundException {
		
		Raca raca = new Raca();
		
		String res;
		int raca_id = Integer.parseInt(id_raca);
		
		try {
//			String sql = "INSERT INTO raca VALUES (DEFAULT, ?)";
			PreparedStatement stm = connection.prepareStatement("UPDATE racas SET rac_nome = ? WHERE rac_iden = ?");
			stm.setString(1, nome_raca);
			stm.setInt(2, raca_id);
			stm.executeUpdate();
			
			res = "Raça alterada com sucesso!";
		} catch (SQLException e) {
			e.printStackTrace();
			res = "Não foi possivel alterar a raça. Erro " + e;
		}
		
		return res;
	}
	
	public String deletarRaca(int id_raca, String nome_raca) throws SQLException, ClassNotFoundException {
		
		Raca raca = new Raca();
		
		String res;
//		int raca_id = Integer.parseInt(id_raca);
		
		try {
//			String sql = "INSERT INTO raca VALUES (DEFAULT, ?)";
			if (id_raca != 0) {
				PreparedStatement stm = connection.prepareStatement("DELETE FROM racas WHERE rac_iden = ?");
				stm.setInt(1, id_raca);
				stm.executeUpdate();				
			} else {
				PreparedStatement stm = connection.prepareStatement("DELETE FROM racas WHERE rac_nome = ?");
				stm.setString(1, nome_raca);
				stm.executeUpdate();	
			}
			
			res = "Raça deletada com sucesso!";
		} catch (SQLException e) {
			e.printStackTrace();
			res = "Não foi possivel deletar a raça. Erro " + e;
		}
		
		return res;
	}
	
	public List<Raca> getAll() {
		
		String sql = "SELECT RAC.RAC_IDEN, RAC_NOME " + "FROM RACAS RAC " + "ORDER BY RAC.RAC_NOME";
		
		//Declarando a Lista de Raças
		List<Raca> racas = new ArrayList<Raca>();
		
		try {
			PreparedStatement stm = connection.prepareStatement(sql);
			
			ResultSet rs = stm.executeQuery();
			
			while (rs.next()) {
				//Enquanto houver registro apontado no resultset...
				Raca raca = new Raca();
				raca.setId(rs.getInt("RAC_IDEN"));
				raca.setNome(rs.getString("RAC_NOME"));
				//Adicionando a raca apontada na lista de raças
				racas.add(raca);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			racas = null;
		}
		
		return racas;
	}

	public int deleteById(int id_raca) {
		String sql = "delete from racas where rac_iden = ?";
		PreparedStatement stm;
		
		try {
			stm = this.connection.prepareStatement(sql);
			stm.setInt(1, id_raca);
			
			int qtd = stm.executeUpdate();
			
			return qtd;
		} catch(SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public void alterarById(Raca alterarRaca) {
		String sql = "update racas set rac_nome = ? where rac_iden = ?";
		PreparedStatement stm;
		
		try {
			stm = this.connection.prepareStatement(sql);
			stm.setString(1, alterarRaca.getNome());
			stm.setInt(2, alterarRaca.getId());
			
			stm.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
			
		}
		
	}
	
}