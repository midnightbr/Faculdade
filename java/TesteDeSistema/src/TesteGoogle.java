/**
 * 
 */
/**
 * @author Marcos Henrique
 *
 */
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class TesteGoogle {
	public static void main(String[] args) {
		System.setProperty("webdriver.chrome.driver", "D:/Programas/chromedriver_win32/chromedriver.exe");
		WebDriver driver = new ChromeDriver();
		driver.get("http://www.google.com");
		System.out.println(driver.getTitle());
		driver.close();
	}
}