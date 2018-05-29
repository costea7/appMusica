/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reproductormúsica;


/**
 *
 * @author Iulian
 */
public class ReproductorMúsicaServidor {

    public static final String NOMBRE_APP = "Aplicación Música";
    public static final int PUERTO_SERVIDOR = 8888;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Servidor servidor = new Servidor(PUERTO_SERVIDOR);
        servidor.start();
        System.out.println("Servidor arrancado en el puerto " + PUERTO_SERVIDOR);
    }

}
