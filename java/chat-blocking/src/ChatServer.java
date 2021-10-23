
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Marcos Henrique
 */
public class ChatServer {
    // Definindo a porta para o servidor
    public static final int PORT = 4000;
    // Instaciando o socket do servidor
    private ServerSocket serverSocket;
    
    // Instanciando o serverSocket
    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        // Mensagem de teste
        System.out.println("Servidor iniciado na porta " + PORT);
        
        clientConnectionLoop();
    }
    
    // Loop infinito para esperar as conexões dos clientes e não permitir
    // que a aplicação feche.
    private void clientConnectionLoop() throws IOException {
        while(true) {
            // Aguarda uma nova conexão para criar um novo socket para o cliente
            // Socket clientSocket = serverSocket.accept();
            ClientSocket clientSocket = new ClientSocket(serverSocket.accept());
            
            // Obter o IP remoto do cliente conectado
            // System.out.println("Cliente " + clientSocket.getRemoteSocketAddress() + " conectou.");
            
            /**
             * Recebimento de dados / Fluxo de recebimento de dados
             * Metodo para poder receber uma String no getInputStream além de salvar dados em buffer
             * Necessario o BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())) devido
             * a que, para poder ter uma quebra de linha, basta apenas colocar .newLine() no final.
             */
            // BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            
            // Lendo uma linha até encontrar o \n
            // String msg = in.readLine();
            
            //Criando uma expressão lambda e uma nova thread
            // new Thread(() -> clientSocket.getMessage()).start();
            new Thread(() -> clientMessageLoop(clientSocket)).start();
            
            /**
             * System.out.println("Mensagem recebida do cliente " + clientSocket.getRemoteSocketAddress()
             * + ": " + msg);
             */
        }
    }
    
    private void clientMessageLoop(ClientSocket clientSocket) {
        String msg;
        try {
            while((msg = clientSocket.getMessage()) != null) {
                
                if("sair".equalsIgnoreCase(msg))
                    return;

                System.out.printf("Msg recebida do cliente %s: %s\n",
                        clientSocket.getRemoteSocketAddress(),
                        msg);
            }    
        } finally {
            clientSocket.close();
        }
        
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {      
        try {
            // Instanciando o Servidor
            ChatServer server = new ChatServer();
            // Chamando o metodo start para iniciar a aplicação
            server.start();
        } catch (IOException ex) {
            // Mensagem caso ocorra um erro
            System.out.println("Erro ao inciar o servidor: " + ex.getMessage());
        }
        
        System.out.println("Servidor finalizado");
    }
    
}
