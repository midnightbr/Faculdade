package ts.clientedao;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
//import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import br.com.ts.dao.ClienteDAO;
import br.com.ts.model.Cliente;

/**
 * Classe de teste criada para garantir o funcionamento
 * das principais operações sobre clientes, realizadas
 * pela classe {@link ClienteDAO}
 * 
 * @author Sandro
 * @date 29/09/2020
*/

public class ClienteDAOTest {
	/**
	 * Teste da pesquisa de um cliente a partir do seu id.
	 */
	@Test
	public void testPesquisaCliente() {
		/* -------------------- Montagem do Cenário -------------------- */
		
		// Criar alguns clientes (objetos)
		Cliente cliente01 = new Cliente(1, "Fulano", 30, "fulano@gmail.com", 1, true);
		Cliente cliente02 = new Cliente(2, "Beltrano", 35, "beltrano@gmail.com", 2, true);
		
		// Inserir estes clientes na base de dados
		List<Cliente> clientesDoBanco = new ArrayList<>();
		clientesDoBanco.add(cliente01);
		clientesDoBanco.add(cliente02);
		
		ClienteDAO clienteDAO = new ClienteDAO(clientesDoBanco);
		
		/* ---------- Execução do Teste ---------- */
		
		Cliente cliente = clienteDAO.pesquisaCliente(2);
		
		/* ---------- Verificação ---------- */
		
		Assert.assertThat(cliente.getId(), is(2));
		Assert.assertThat(cliente.getEmail(), is("beltrano@gmail.com"));
	}
	
	/**
	 * Teste de remoção de um cliente a partir do seu id.
	 */
	@Test
	public void testRemoveCliente() {
		/* ---------- Montagem do Cenário ---------- */
		
		// Criar alguns Clientes (objetos)
		Cliente cliente01 = new Cliente(1, "Fulano", 30, "fulano@gmail.com", 1, true);
		Cliente cliente02 = new Cliente(2, "Beltrano", 35, "beltrano@gmail.com", 2, true);
		
		// Inserir estes clientes na base de dados
		List<Cliente> clientesDoBanco = new ArrayList<>();
		clientesDoBanco.add(cliente01);
		clientesDoBanco.add(cliente02);
		
		ClienteDAO clienteDAO = new ClienteDAO(clientesDoBanco);
		
		/* ---------- Execução do Teste ---------- */
		boolean clienteRemovido = clienteDAO.removeCliente(2);
		
		/* ---------- Verificação ---------- */
		Assert.assertThat(clienteRemovido, is(true));
		Assert.assertThat(clienteDAO.getClientesDoBanco().size(), is(1));
		Assert.assertNull(clienteDAO.pesquisaCliente(2));
		
	}
}