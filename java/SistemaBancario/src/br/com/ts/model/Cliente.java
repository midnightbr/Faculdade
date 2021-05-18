package br.com.ts.model;

/**
 * Classe {@link Cliente} representa um cliente do banco.
 * @author Sandro
 */
public class Cliente {

	private int id;
	private String nome;
	private int idade;
	private String email;
	private boolean ativo;
	private int idContaCorrente;

	public Cliente(int id, String nome, int idade, String email, int idContaCorrente, boolean ativo) {
		this.id = id;
		this.nome = nome;
		this.idade = idade;
		this.email = email;
		this.idContaCorrente = idContaCorrente;
		this.ativo = ativo;
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public int getIdade() {
		return idade;
	}

	public void setIdade(int idade) {
		this.idade = idade;
	}
	
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public void setAtivo(boolean ativo) {
		this.ativo = ativo;
	}

	public int getIdContaCorrente() {
		return this.idContaCorrente;
	}

	public void setIdContaCorrente(int idContaCorrente) {
		this.idContaCorrente = idContaCorrente;
	}
	
	@Override
	public String toString() {
		
		String str ="=========================\n" 
					+"Id: " + this.id + "\n"
					+ "Nome: " + this.nome + "\n"
					+ "Email: " + this.email + "\n"
					+ "Idade: " + this.idade + "\n"
					+ "Status: " + (ativo?"Ativo":"Inativo") + "\n"
					+ "=========================";
		return str;
	}
}
