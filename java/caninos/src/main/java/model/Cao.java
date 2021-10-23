/**
 * 
 */
/**
 * @author marcos
 *
 */
package model;

public class Cao {
	
	private int id;
	private String nome;
	private String sexo;
	private Raca raca;
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getNome() {
		return nome;
	}
	
	public void setNome (String nome) {
		this.nome = nome;
	}
	
	public String getSexo() {
		return sexo;
	}
	
	public void setSexo (String sexo) {
		this.sexo = sexo;
	}
	
	public Raca getRaca() {
		return raca;
	}
	
	public void setRaca(Raca raca) {
		this.raca = raca;
	}
	
	@Override
	public String toString() {
		return "Cao [id = " + id + " | Nome = " + nome + " | Sexo = " + sexo + " | Raça = " + raca + " ]";
	}
}