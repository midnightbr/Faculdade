/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;

/**
 *
 * @author Marcos Henrique
 */
public class ClientSocket {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    
    public ClientSocket(Socket socket) throws IOException {
        this.socket = socket;
        
        System.out.println("Cliente " + socket.getRemoteSocketAddress() + " conectou.");
       
        // Método do lado do servidor
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
       
        // Método do lado do cliente
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }
    
    public SocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();
    }
    
    public void close() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch(IOException e) {
            System.out.println("Erro ao fechar socket: " + e.getMessage());
        }
        
    }
    
    public String getMessage() {
        try {
            return in.readLine();
        } catch(IOException e) {
            return null;
        } 
    }
    
    public boolean sendMsg(String msg) {
        out.println(msg);
        return !out.checkError();
    }
}
