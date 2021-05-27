package testeLogin;
/**
 * 
 */
/**
 * @author Marcos Henrique
 *
 */
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class TesteLogin {
	/*
	 * Teste ilustando o acionamento automatizado de um botão de uma página web através do método click
	 * do objeto WebElement do Selenium WebDriver.
	 */
	@Test
	public void testButtonCliqueAqui() {
		System.setProperty("webdriver.chrome.driver", "D:/Programas/chromedriver_win32/chromedriver.exe");
		WebDriver driver = new ChromeDriver();
		
		WebElement botao = driver.findElement(By.id("idBtnCliqueAqui"));
		botao.click();
		Assert.assertEquals("OK", botao.getAttribute("value"));
	}
	
	/*
	 * Teste simples em uma paginá de login, que consiste na digitação automatizada do nome de usuário "Sandro"
	 * na caixa de texto cujo id no código HTML5 é "usuario" e, em seguida, fazer uma verificação(assertEquals) se o nome
	 * digitado foi exatamente o nome de usuário que está presente na caixa de texto.
	 */
	@Test
	public void testeTextField() {
		System.setProperty("webdriver.chrome.driver", "D:/Programas/chromedriver_win32/chromedriver.exe");
		WebDriver driver = new ChromeDriver();
		
		driver.manage().window().setSize(new Dimension(800, 600));
		driver.get("file:///" + System.getProperty("user.dir") + "/src/main/resources/login.html");
		driver.findElement(By.id("usuario")).sendKeys("Sandro");
		
		Assert.assertEquals("Sandro", driver.findElement(By.id("usuario")).getAttribute("value"));
		
		driver.quit();
	}
}