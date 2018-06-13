package reproductormúsica;

/**
 *
 * @author Iulian
 *
 * @version 1.0 Implementación
 */
public class ReproductorMúsicaServidor {

    public static final String NOMBRE_APP = "Aplicación Música";
    public static final int PUERTO_SERVIDOR = 8833;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Servidor servidor = new Servidor(PUERTO_SERVIDOR);
        servidor.start();
        System.out.println("Servidor arrancado en el puerto " + PUERTO_SERVIDOR);
    }
}
