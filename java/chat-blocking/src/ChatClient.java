
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Marcos Henrique
 */
public class ChatClient {
    // Instanciando o IP do servidor
    private static final String SERVER_ADDRESS = "127.0.0.1";
    // Instaciando o socket do cliente
    private Socket clientSocket;
    private Scanner scanner;
    // private BufferedWriter out;
    private PrintWriter out;
    
    public ChatClient() {
        // Instanciando um scanner para capturar dados de entrada
        scanner = new Scanner(System.in);
        
    }
    
    // Instanciando o clientSocket com o IP e PORTA do servidor
    public void start() throws IOException {
        clientSocket = new Socket(SERVER_ADDRESS, ChatServer.PORT);
        
        /**
         * Envio de dados / Fluxo de envio de dados
         * Metodo para poder enviar uma String no getOutputStream além de salvar dados em buffer
         * Necessario o new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())) devido
         * a que, para poder ter uma quebra de linha, basta apenas colocar .newLine() no final.
         */
        // this.out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        
        // Tem a mesma função do código: new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
        // this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        
        /**
         * OBS.: Devido ao fato de que o PrintWriter ter o autoflush embutido em si,
         * bastou colocar o valor true no código acima para não precisar mais do uso
         * to out.flush() no metodo messageLoop.
         */
        
        clientSocket.getOutputStream();
        // Mensagem de teste da conexão
        System.out.println("Cliente conectado ao servidor em " + SERVER_ADDRESS + ":" + ChatServer.PORT);
        
        messageLoop();
    }
    
    // Loop infinito para esperar as mensagens do servidor e não permitir
    // que a aplicação feche.
    private void messageLoop() throws IOException {
        String msg;
        do {
            // Capturando os dados digitados do usuario
            System.out.print("Digite uma messagem (ou sair para finalizar): ");
            msg = scanner.nextLine();
            // Envio da Mensagem
            //out.write(msg);
            out.println(msg); //Subtitui o write() e o newLine()
            
            // Quebra de linha
            // out.newLine();
            
            // Confirmar o envio ou enviar de imediato a mensagem
            //out.flush();
        } while(!msg.equalsIgnoreCase("sair")); // Metodo de saida
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        try {
            // Instanciando o cliente
            ChatClient client = new ChatClient();
            // Chamando o metodo start para iniciar a aplicação
            client.start();
        } catch (IOException ex) {
            System.out.println("Erro ao iniciar cliente: " + ex.getMessage());
        }
        
        System.out.println("Cliente finalizado!");
    }
    
}
