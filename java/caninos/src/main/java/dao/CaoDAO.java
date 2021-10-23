package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import database.ConnectionFactory;
import model.Cao;
import model.Raca;

public class CaoDAO {

	private Connection connection;
	
	public CaoDAO() {
		this.connection = new ConnectionFactory().getConnection();
	}
	
	public Cao getById(int id) {
		String sql = "SELECT CAO.CAO_IDEN, CAO_RAC_IDEN, CAO_NOME, CAO_SEXO, " + "FROM CAES CAO" + " WHERE CAO.CAO_IDEN = " + id;
		
		Cao cao = new Cao();
		
		try {
			PreparedStatement stm = connection.prepareStatement(sql);
			ResultSet rs = stm.executeQuery();
			
			while (rs.next()) {
				cao.setId(rs.getInt("CAO_IDEN"));
				cao.setNome(rs.getString("CAO_NOME"));
				cao.setSexo(rs.getString("CAO_SEXO"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			cao = null;
		}
		
		return cao;
	}

	public List<Cao> getAll() {
		String sql = "SELECT CAO.CAO_IDEN, CAO_RAC_IDEN, CAO_NOME, CAO_SEXO, " + 
				"RAC.RAC_IDEN, RAC_NOME " + "FROM CAES CAO INNER JOIN RACAS RAC " + "ON CAO.CAO_RAC_IDEN = RAC.RAC_IDEN " + "ORDER BY CAO.CAO_NOME";

		List<Cao> caes = new ArrayList<Cao>();
		
		try {
			PreparedStatement stm = this.connection.prepareStatement(sql);
			ResultSet rs = stm.executeQuery();
			
			while(rs.next()) {
				Cao cao = new Cao();
				Raca raca = new Raca();
				
				raca.setId(rs.getInt("RAC_IDEN"));
				raca.setNome(rs.getString("RAC_NOME"));
				cao.setId(rs.getInt("CAO_IDEN"));
				cao.setNome(rs.getString("CAO_NOME"));
				cao.setSexo(rs.getString("CAO_SEXO"));
				cao.setRaca(raca); // Aqui incluimos o objeto raca no objeto cão
				
				caes.add(cao); // Incluindo o cão apontando na lista de cães
			}
		} catch (SQLException e) {
			e.printStackTrace();
			caes = null;
		}
		
		return caes;

	}
	
}
