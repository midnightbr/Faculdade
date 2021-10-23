package socket;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class Servidor {
	
	public static void main(String[] args) throws UnknownHostException, IOException {
		ServerSocket serverSocket = new ServerSocket(8001);
		Socket socket = serverSocket.accept();
		System.out.println("Conexão aceita.\nCliente conectado!");

		InputStreamReader in = new InputStreamReader(socket.getInputStream());
		BufferedReader bf = new BufferedReader(in);
		String str = bf.readLine();
		System.out.println("Servidor recebeu esta mensagem do cliente: " + str);
		
		PrintWriter pr = new PrintWriter(socket.getOutputStream());
		pr.println("Ola cliente, eu vim do servidor!");
		pr.flush();
		
		serverSocket.close();
		socket.close();
	}
	
}