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
	 * Teste ilustando o acionamento automatizado de um bot�o de uma p�gina web atrav�s do m�todo click
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
	 * Teste simples em uma pagin� de login, que consiste na digita��o automatizada do nome de usu�rio "Sandro"
	 * na caixa de texto cujo id no c�digo HTML5 � "usuario" e, em seguida, fazer uma verifica��o(assertEquals) se o nome
	 * digitado foi exatamente o nome de usu�rio que est� presente na caixa de texto.
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