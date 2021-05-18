package br.com.ts.dao;

import java.util.List;

import br.com.ts.model.Cliente;

/**
 * Classe de neg�cio para realizar opera��es sobre os clientes do banco.
 * @author Sandro
 */
public class ClienteDAO {

	private List<Cliente> clientesDoBanco;

	public ClienteDAO(List<Cliente> clientesDoBanco) {
		this.clientesDoBanco = clientesDoBanco;
	}
	
	/**
	 * @return lista com todos os clientes do banco
	 */
	public List<Cliente> getClientesDoBanco() {
		return clientesDoBanco;
	}
	
	/**
	 * Pesquisa cliente a partir do ID.
	 * @param idCliente
	 * @return cliente pesquisado ou null, caso n�o encontrado
	 */
	public Cliente pesquisaCliente (int idCliente) {
		for (Cliente cliente : clientesDoBanco) {
			if (cliente.getId() == idCliente)
				return cliente;
		}
		return null;
	}
	
	/**
	 * Adiciona novo cliente � lista de clientes do banco.
	 * @param novoCliente cliente a ser adicionado
	 */
	public void adicionaCliente (Cliente novoCliente) {
		clientesDoBanco.add(novoCliente);
	}

	/**
	 * Remove cliente da lista de clientes do banco.
	 * @param idCliente 
	 * @return true se removido ou false, caso contr�rio.
	 */
	public boolean removeCliente (int idCliente) {
		boolean clienteRemovido = false;
		
		for (int i = 0; i < clientesDoBanco.size(); i++) {
			Cliente cliente = clientesDoBanco.get(i);
			if (cliente.getId() == idCliente) {
				clientesDoBanco.remove(i);
				clienteRemovido = true;
				break;
			}
		}
		
		return clienteRemovido;
	}

	/**
	 * Informa se cliente est� ativo ou n�o.
	 * @param idCliente
	 * @return true se cliente ativo ou false, caso contr�rio. 
	 */
	public boolean clienteAtivo(int idCliente) {
		boolean clienteAtivo = false;
		
		for (int i = 0; i < clientesDoBanco.size(); i++) {
			Cliente cliente = clientesDoBanco.get(i);
			
			if (cliente.getId() == idCliente)
				if (cliente.isAtivo()) {
					clienteAtivo = true;
					break;
				}
		}
		
		return clienteAtivo;
	}

	/**
	 * Limpa lista de clientes 
	 */
	public void limpa() {
		this.clientesDoBanco.clear();
	}
	

}
