package ts.clientedao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Suite;

//import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import br.com.ts.dao.ClienteDAO;
import br.com.ts.model.Cliente;

/**
 * Classe de teste criada para garantir o funcionamento
 * das principais opera��es sobre clientes, realizadas
 * pela classe {@link ClienteDAO}
 * 
 * @author Sandro
 * @date 29/09/2020
*/

/*@RunWith(Suite.class)
@SuiteClasses({ ClienteDAOTest.class, ProdutoDAOTest.class,
				PedidoDAOTest.class, VendaServiceTest.class })

public class TestSuit {
	@BeforeClass
	public static void setup() {
		
	}
	
	@AfterClass
	public static void close() {
		
	}
}
*/

@FixMethodOrder(MethodSorters.NAME_ASCENDING) // Defini a ordem de execu��o dos testes em ordem alfabetica
public class ClienteDAOTest {	

	//Variaveis declaradas para a execu��o com sucesso a classe setUp
	private ClienteDAO clienteDAO;
	private int idCliente01 = 1;
	private int idCliente02 = 2;

	/**
	 * Classe no qual vai configurar e adicionar dados ao banco de dados para os testes.
	 */
	
	@Before // Significa que vai ser executado antes que qualquer teste
	public void setUp() {
		/* ========== Montagem do cen�rio ========== */
		
		// Cria alguns clientes
		Cliente cliente01 = new Cliente(idCliente01 , "Fulano", 31, "fulano@gmail.com", 1, true);
		Cliente cliente02 = new Cliente(idCliente02 , "Beltrano", 34, "beltrano@gmail.com", 1, true);
		
		// Insere os clientes criados na lista de clientes do banco
		List<Cliente> clientesDoBanco = new ArrayList<>();
		clientesDoBanco.add(cliente01);
		clientesDoBanco.add(cliente02);
		
		clienteDAO = new ClienteDAO(clientesDoBanco);
	}
	
	/*
	 * Classe no qual vai apagar qualquer vestigio no banco do teste executado anteriormente
	 */
	@After // Significa que vai ser executado depois de cada teste
	public void tearDown() {
		clienteDAO.limpa();
	}
	
	/*
	 * Teste b�sico da pesquisa de um cliente a partir do seu ID.
	 */
	
	@Test
	public void testPesquisaCliente() {
		
		/* ========== Execu��o ========== */
		Cliente cliente = clienteDAO.pesquisaCliente(idCliente01);
		
		/* ========== Verifica��es ========== */
		/*
		 * Fun��o assertThat, aceita dois argumentos, sendo o primeiro um valor
		 * gerado a partir da execu��o do teste e o segundo um valor esperado desse
		 * teste.
		 */
		Assert.assertThat(cliente.getId(), is(idCliente01));
		
		/*
		 * Metodo de teste para compara��o de objetos. 
		 * Se forem o mesmo objeto, a mensagem de erro aparecera.
		 */
		// Assert.assertNotSame("N�o deve ser o mesmo objeto", cliente, cliente);
		
		/*
		 * Tem o mesmo objetivo do assertNotSame de comparar dois objetos.
		 * No entanto inves de dar erro, ele espera que os dois objetos sejam iguais.
		 * Como o objeto inteiro1 � apontado pelo objeto inteiro2, ent�o o teste
		 * passara com sucesso.
		 */
		Integer inteiro1 = 12345;
		Integer inteiro2 = inteiro1;
		Assert.assertSame("Devem ser o mesmo", inteiro1, inteiro2);
		
		/*
		 * O metodo assertArrayEquals mostrara a mensagem de erro se os dois
		 * argumentos (arrays) forem diferentes um do outro. Caso seja iguais o resultado
		 * sera de sucesso.
		 */
		byte[] esperado = "teste de software".getBytes();
		byte[] atual = "teste de software".getBytes();
		Assert.assertArrayEquals("Falha - os arrays de bytes n�o s�o iguais.", esperado, atual);
		
		/*
		 * O metodo assertEquals mostrar� a mensagem de falha se as strings
		 * dos pr�ximos dois argumentos forem diferentes.
		 */
		Assert.assertEquals("Falha - strings n�o s�o iguais", "teste de software", "teste de software");
		// Assert.assertEquals("Falha - strings n�o s�o iguais", "teste de software", "Tekur Miner");
		
		/*
		 * Os m�todos assertTrue e assertFalse s�o muito �teis em situa��es
		 * em que precisa-se avaliar um teste booleano.
		 */
		Assert.assertTrue("Falha - deve ser true", true);
		Assert.assertFalse("Falha - deve ser false", false);
		
		/*
		 * O exemplo a seguir ilustra o uso dos m�todos both(ambos), containsString(cont�m string)
		 * e and(e).
		 */
		Assert.assertThat("Teste de software", both(containsString("e")).and(containsString("s")));
		
		/*
		 * O pr�ximo exemplo verifica se as palavras "teste" e "software" est�o contidas no array.
		 */
		Assert.assertThat(Arrays.asList("teste", "de", "software"), hasItems("teste", "software"));
		
		/*
		 * No exemplo a seguir, o m�todo everyItem, que pode ser traduzido como cada, tamb�m resulta
		 * em sucesso, porque cada uma das tr�s palavras contidas no array de strings possui a letra "e".
		 */
		Assert.assertThat(Arrays.asList(new String [] {"teste", "de", "software"}), everyItem(containsString("e")));
		
		/*
		 * A primeira das fun��es abaixo, verifica se a string do primeiro argumento, se igual a "teste" e se inicia 
		 * por "tes", o que � verdadeiro
		 */
		Assert.assertThat("teste", allOf(equalTo("teste"), startsWith("tes")));
		
		/*
		 * Na proxima fun��o abaixo, perceba que a express�o not() agrupa todo o seu conte�do e, sabe-se
		 * que a string "de" n�o existe no primeiro argumento "teste", mas a string em equalTo existe, retornando
		 * falso. Por�m, a express�o not() inverte a proposi��o fazendo com que o resultado da asser��o seja sucesso.
		 */
		Assert.assertThat("teste", not(allOf(equalTo("de"), equalTo("teste"))));
		
		/*
		 * Na fun��o abaixo ilustra o uso de anyOf(qualquer de) que funciona como uma disjun��o, ou seja, se "de" ou
		 * "teste" for igual ao primeiro argumento ("teste"), ent�o o resultado da asser��o ser� sucesso.
		 */
		Assert.assertThat("teste", anyOf(equalTo("de"), equalTo("teste")));
	}
	
	/**
	 * Teste de remo��o de um cliente a partir do seu id.
	 */
	@Test
	public void testRemoveCliente() {
		
		/* ---------- Execu��o do Teste ---------- */
		boolean clienteRemovido = clienteDAO.removeCliente(idCliente02);
		
		/* ---------- Verifica��o ---------- */
		Assert.assertThat(clienteRemovido, is(true));
		Assert.assertThat(clienteDAO.getClientesDoBanco(). size(), is(1));
		/*
		 * M�todo assertNull, que retorna o valor null se n�o encontrar o objeto
		 * pesquisado.
		 */
		Assert.assertNull(clienteDAO.pesquisaCliente(idCliente02));
		
		/*
		 * Diferente do assertNull, esse teste ele espera que o retorno n�o seja
		 * do tipo null, como no teste anterior. Por isso, caso seja retornado o valor null,
		 * o teste sera retornado com um erro.
		 */
		// Assert.assertNotNull("N�o deve ser null", clienteDAO.pesquisaCliente(idCliente02));
		
	}
}