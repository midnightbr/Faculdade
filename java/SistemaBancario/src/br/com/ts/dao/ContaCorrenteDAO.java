package br.com.ts.dao;

import java.util.List;

import br.com.ts.model.ContaCorrente;

/**
 * Classe de neg�cio para realizar opera��es sobre as contas do banco.
 * @author Sandro
 */
public class ContaCorrenteDAO {

	private List<ContaCorrente> contasDoBanco;

	public ContaCorrenteDAO(List<ContaCorrente> contasDoBanco) {
		this.contasDoBanco = contasDoBanco;
	}

	/**
	 * Retorna lista com todas as contas do banco.
	 * @return lista
	 */
	public List<ContaCorrente> getContasDoBanco() {
		return contasDoBanco;
	}

	/**
	 * Pesquisa conta por ID.
	 * @param idConta
	 * @return conta pesquisada ou null, caso n�o encontrada
	 */
	public ContaCorrente pesquisaConta (int idConta) {

		for (ContaCorrente contaCorrente : contasDoBanco) {
			if (contaCorrente.getId() == idConta)
				return contaCorrente;
		}
		
		return null;
	}
	
	/**
	 * Adiciona nova conta � lista de contas do banco.
	 * @param novaConta
	 */
	public void adicionaConta (ContaCorrente novaConta) {
		this.contasDoBanco.add(novaConta);
	}

	/**
	 * Remove conta da lista de contas do banco.
	 * @param idConta 
	 * @return true se conta removida ou false, caso contr�rio.
	 */
	public boolean removeConta (int idConta) {
		
		boolean contaRemovida = false;
		
		for (int i = 0; i < contasDoBanco.size(); i++) {
			ContaCorrente conta = contasDoBanco.get(i);

			if (conta.getId() == idConta) {
				contasDoBanco.remove(i);
				break;
			}
		}
		
		return contaRemovida;
	}

	/**
	 * Informa se determinada conta est� ativa.
	 * @param idConta
	 * @return true se conta ativa ou false, caso contr�rio. 
	 */
	public boolean contaAtiva (int idConta) {
		
		boolean contaAtiva = false;
		
		for (int i = 0; i < contasDoBanco.size(); i++) {
			ContaCorrente conta = contasDoBanco.get(i);
			
			if (conta.getId() == idConta)
				if (conta.isAtiva()) {
					contaAtiva = true;
					break;
				}
		}
		
		return contaAtiva;
	}
	
	/**
	 * Transfere um determinado valor de uma conta Origem para uma conta Destino.
	 * Se saldo insuficiente, valor n�o ser� transferido.
	 * 
	 * @param idContaOrigem conta que ter� o valor deduzido
	 * @param valor valor a ser transferido
	 * @param idContaDestino conta que ter� o valor acrescido
	 * @return true, se a transfer�ncia realizada com sucesso.
	 */
	public boolean transfereValor (int idContaOrigem, double valor, int idContaDestino) {
		boolean sucesso = false;
		
		ContaCorrente contaOrigem = pesquisaConta(idContaOrigem);
		ContaCorrente contaDestino = pesquisaConta(idContaDestino);
		
		if (contaOrigem.getSaldo() >= valor) {
			contaDestino.setSaldo(contaDestino.getSaldo() + valor);
			contaOrigem.setSaldo(contaOrigem.getSaldo() - valor);
			sucesso = true;
		}
	
		return sucesso;
	}
}
