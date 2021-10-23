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
		String url =  "jdbc:mysql://localhost:3306/tekur?useTimezone=true&serverTimezone=UTC";
		String usuario = "root";
		String senha = "beta2209";
		try {
			Class.forName(driver);
			conexao = DriverManager.getConnection(url, usuario, senha);
			if(conexao != null)
				System.out.println("Conex�o aberta com sucesso!");
			
			return conexao;
		} catch(Exception ex) {
			System.out.println(ex);
			System.out.println("N�o foi possivel realizar a conex�o com o BD");
			
			return null;
		}
	}
}