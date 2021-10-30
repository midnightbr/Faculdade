/**
 * 
 */
/**
 * @author marcos
 *
 */
package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
	public Connection getConnection() {
		
		String url = "jdbc:postgresql://localhost:5432/db_caninos";
		String user = "admin";
		String password = "beta2209";
		
		try {
			Class.forName("org.postgresql.Driver");
			return DriverManager.getConnection(url, user, password);
		} catch (SQLException | ClassNotFoundException e) {
			// Todo Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}