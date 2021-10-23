package cliente;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Cliente {
	
	public static void main(String[] args) throws IOException {
		Socket socket = new Socket("localhost", 8001);
		System.out.println("Requisicao de conexao enviada ao servidor.");
		
		// Configurando uma mensagem para ser enviada ao servidor via socket
		PrintWriter pr = new PrintWriter(socket.getOutputStream());
		// Mensagem
		pr.println("Ola servido, eu vim do cliente!");
		// Comando flush que garante que a mensagem seja enviada.
		pr.flush();
		
		InputStreamReader in = new InputStreamReader(socket.getInputStream());
		BufferedReader bf = new BufferedReader(in);
		String str = bf.readLine();
		System.out.println("Cliente recebu esta mensagem do servidor: " + str);
		
		socket.close();
	}
}