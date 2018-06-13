package reproductormúsica;

/**
 *
 * @author Iulian
 */
public interface MensajesJSON {

    public enum type {
        LOGIN, REGISTRO, CERRAR_SESION,
        VER_CONECTADOS, ENVIAR_MENSAJE,
        RECIBIR_MENSAJE, VER_CANCIONES, VER_CANCIONES_USER, DESCARGAR_CANCION,
        SUBIR_CANCION, PISTA_ACTUAL, NUEVO_USUARIO, SALIR
    };

    final String TYPE = "type";
    final String VALUES = "values";

    final String ANSWER = "answer";

    final String USER = "user";
    final String DESTINATARIO = "destinatario";
    final String PASS = "nPass";
    final String IP = "ip";

    final String SOCKET = "socket";

    final String RUTA_CANCION = "ruta";
    final String NOM_CANCION = "nombreCancion";

    final String TO = "to";

    final String LIST = "list";
    final String LISTC = "listc";

    final String TEXT = "text";
}
