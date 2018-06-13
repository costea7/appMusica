package reproductormúsica;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 *
 * @author Iulian
 * 
 * @version 1.0 Definición y declaración métodos
 */

public class Servidor extends Thread {

    private ServerSocket serversocket;
    private Socket socketCliente;
    private ArrayList listaCliente;
    private GestorBD baseDatos;

    public Servidor(int port) {
        try {
            serversocket = new ServerSocket(port);
        } catch (IOException ex) {
            System.out.println("error al conectarse");
        }
        this.baseDatos = GestorBD.getInstancia();
        listaCliente = new ArrayList<>();
    }

    public GestorBD getDB() {
        return baseDatos;
    }

    public void run() {
        while (true) {
            try {
                socketCliente = serversocket.accept();
                System.out.println("nuevo usuario conectado ");
                HiloEjecucion hilo = new HiloEjecucion(this, socketCliente);
                hilo.start();
            } catch (IOException e) {
                System.out.println("error conexion cliente");
            }
        }
    }
}
