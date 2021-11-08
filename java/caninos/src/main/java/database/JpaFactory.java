package database;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class JpaFactory {
	
	private static EntityManagerFactory factory;
	
	static {
		factory = Persistence.createEntityManagerFactory("CaninosPU");
	}
	
	public static EntityManager getEntityManager() {
		return factory.createEntityManager();
	}
}