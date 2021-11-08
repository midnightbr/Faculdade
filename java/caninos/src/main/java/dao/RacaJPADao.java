package dao;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import database.JpaFactory;
import model.Raca;
import java.util.List;

public class RacaJPADao {
	
	private EntityManager manager;
	
	public RacaJPADao() {
		this.manager = JpaFactory.getEntityManager();
	}
	
	public void add(Raca raca) {
		EntityTransaction tx = manager.getTransaction();
		tx.begin();
		
		manager.persist(raca);
		
		tx.commit();
	}
	
	public void update(Raca raca) {
		EntityTransaction tx = manager.getTransaction();
		tx.begin();
		
		Raca minhaRaca = manager.find(Raca.class, raca.getId());
		minhaRaca.setNome(raca.getNome());
		tx.commit();
	}
	
	public void delete(Raca raca) {
		EntityTransaction tx = manager.getTransaction();
		tx.begin();
		
		Raca minhaRaca = manager.find(Raca.class, raca.getId());
		manager.remove(minhaRaca);
		tx.commit();
	}
	
	public List<Raca> getAll() {		
		Query query = manager.createQuery("from Raca");
		List<Raca> racas = query.getResultList();
		return racas;
	}
}
