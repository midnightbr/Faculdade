/**
 * 
 */
/**
 * @author Marcos Henrique
 *
 */
package DatabaseConnection;

import java.sql.Connection;
import java.sql.DriverManager;

public class Conexao {
	
	public static Connection conector() {
		java.sql.Connection conexao = null;
		String driver = "com.mysql.cj.jdbc.Driver";
		String url = "jdbc:mysql://localhost:3377/pet?useTimezone=true&serverTimezone=UTC";
		String usuario = "root";
		String senha = "beta2209";
		try {
			Class.forName(driver);
			conexao = DriverManager.getConnection(url, usuario, senha);
			if(conexao != null)
				System.out.println("Conexão aberta com sucesso!");
			
			return conexao;
		} catch(Exception ex) {
			System.out.println(ex);
			System.out.println("Não foi possivel realizar a conexão com o banco!");
			
			return null;
		}
	}
}