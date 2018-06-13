package reproductormúsica;

/**
 *
 * @author Iulian
 *
 * @version 1.0 Declaracion de metodos
 *
 * @version 2.0 Implementadas pimeras funcionalidades
 * @version 2.1 Comunicación Cliente-Servidor-Cliente vía socket
 * @version 2.2 Login y Registro usuario con JSON
 * @version 2.3 Implementado método para obtener listado con los usuarios
 *
 * @version 3.0 Implementada parte subida-descarga canciones
 * @version 3.1 Método descargaCancion, funcional
 * @version 3.1 Método subirCancion, funcional
 *
 * @version 4.0 Chat y Cancion Actual
 *
 */
import archivos.Clientes_Server.MensajeDameFichero;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.io.ObjectInputStream;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import archivos.Server_Cliente.MensajeTomaFichero;
import archivos.Server_Cliente.hilorecibe;
import archivos.Clientes_Server.hiloenvio;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 *
 * @author Iulian
 */
public class HiloEjecucion extends Thread implements Runnable {

    private final Socket socket;
    private final Servidor servidor;
    private DataInputStream in;
    private DataOutputStream out;
    private ArrayList<String> listamensajes;

    JSONObject mensaje = null;

    private String usuario;

    public HiloEjecucion(Servidor servidor, Socket cliente) throws IOException {
        socket = cliente;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        listamensajes = new ArrayList<>();
        this.servidor = servidor;
    }

    @Override
    public void run() {
        JSONObject json;
        JSONParser parser = new JSONParser();

        String cadena = "";

        while (!cadena.equals("Salir")) {
            try {
                cadena = in.readUTF();
                //System.out.println("dale " + cadena);
                json = (JSONObject) parser.parse(cadena);
                mensaje = gestionReproductor(json);
            } catch (IOException ex) {
                Logger.getLogger(HiloEjecucion.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ParseException ex) {
                Logger.getLogger(HiloEjecucion.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(HiloEjecucion.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SQLException ex) {
                Logger.getLogger(HiloEjecucion.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                out.writeUTF(mensaje.toString());
                out.flush();
            } catch (IOException e) {
                System.out.println("hilo, error de escritura " + e.getMessage());
            } catch (NullPointerException e) {

            }
        }
    }

    public JSONObject gestionReproductor(JSONObject mensaje) throws IOException, ClassNotFoundException, SQLException, ParseException {
        JSONObject resp = null;
        switch (MensajesJSON.type.valueOf((String) mensaje.get(MensajesJSON.TYPE))) {
            case LOGIN:
                resp = loginUsuario(mensaje);
                break;
            case REGISTRO:
                resp = registrarUsuario(mensaje);
                break;
            case CERRAR_SESION:
                cerrarSesion();
                break;
            case VER_CONECTADOS:
                resp = verUsuarios();
                break;
            case ENVIAR_MENSAJE:
                //enviarMensajeChat(mensaje);
                break;
            case RECIBIR_MENSAJE:
                //recibirMensajeChat();
                break;
            case VER_CANCIONES:
                resp = verListaCanciones();
                break;
            case VER_CANCIONES_USER:
                resp = verCancionesUsuario();
                break;
            case DESCARGAR_CANCION:
                resp = descargarCancion(mensaje, socket);
                break;
            case SUBIR_CANCION:
                resp = subirCancion(mensaje, socket);
                break;
            case PISTA_ACTUAL:
                // resp = saberPistaActual(mensaje);
                break;
            case SALIR:
                resp = salir(mensaje);
                break;
        }
        return resp;
    }

    public JSONObject registrarUsuario(JSONObject mensaje) throws IOException {
        JSONObject valores = (JSONObject) mensaje.get(MensajesJSON.VALUES);

        String usuario = (String) valores.get(MensajesJSON.USER);
        String pass = (String) valores.get(MensajesJSON.PASS);
        System.out.println("confirmación registro: " + usuario + " - " + pass);

        boolean respuesta = servidor.getDB().registrarUsuario(usuario, pass);

        if (respuesta) {
            return crearMsjNuevoUsuario(respuesta);
        } else {
            return crearMsjENuevoUsuario(respuesta);
        }
    }

    public JSONObject subirCancion(JSONObject mensaje, Socket cliente) throws IOException, ClassNotFoundException, SQLException {

        JSONObject valores = (JSONObject) mensaje.get(MensajesJSON.VALUES);
        String nombreCancion = (String) valores.get(MensajesJSON.NOM_CANCION);
        String ruta = (String) valores.get(MensajesJSON.RUTA_CANCION);

        boolean respuesta = servidor.getDB().subirCancion(nombreCancion, ruta);

        ObjectInputStream ois = new ObjectInputStream(cliente.getInputStream());
        Object cancion = ois.readObject();

        if (cancion instanceof MensajeTomaFichero) {

            hilorecibe hr = new hilorecibe(((MensajeTomaFichero) cancion).nombreFichero, ois);
            hr.run();

        }

        HiloEjecucion hh = new HiloEjecucion(servidor, cliente);
        hh.run();

        if (respuesta) {
            return (crearMsjSubirCancion(respuesta));
        } else {
            return (crearMsjESubirCancion(respuesta));
        }
    }

    private JSONObject descargarCancion(JSONObject mensaje, Socket cliente) throws IOException, IOException, ClassNotFoundException {

        JSONObject valores = (JSONObject) mensaje.get(MensajesJSON.VALUES);
        String nombreUsuario = (String) valores.get(MensajesJSON.USER);
        String nombreCancion = (String) valores.get(MensajesJSON.NOM_CANCION);

        boolean respuesta = servidor.getDB().descargarCancion(nombreUsuario, nombreCancion);

        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(cliente.getInputStream());
            Object cancion = ois.readObject();

            hiloenvio hiloenv = new hiloenvio(((MensajeDameFichero) cancion).nombreFichero, new ObjectOutputStream(cliente.getOutputStream()));
            hiloenv.run();
        } catch (IOException | ClassNotFoundException ex) {

        }

        if (respuesta) {
            return (crearMsjDescargarCancion(respuesta));
        } else {
            return (crearMsjEDescargarCancion(respuesta));
        }

    }

    public JSONObject loginUsuario(JSONObject mensaje) throws IOException {
        JSONObject valores = (JSONObject) mensaje.get(MensajesJSON.VALUES);

        String usuario = (String) valores.get(MensajesJSON.USER);
        String pass = (String) valores.get(MensajesJSON.PASS);
        System.out.println("confirmación login: " + usuario + " - " + pass);

        boolean respuesta = servidor.getDB().comprobarAutentificacion(usuario, pass);

        if (respuesta) {

            return crearMsjLoginUsuario(respuesta);
        } else {

            return crearMsjELoginUsuario(respuesta);
        }

    }

    private JSONObject crearMsjNuevoUsuario(boolean exito) {
        JSONObject respuesta = new JSONObject();

        //  respuesta.put(MensajesJSON.TYPE, MensajesJSON.type.NUEVO_USUARIO.toString());
        respuesta.put(MensajesJSON.VALUES, exito);

        return respuesta;
    }

    private JSONObject crearMsjENuevoUsuario(boolean exito) {
        JSONObject respuesta = new JSONObject();

        //  respuesta.put(MensajesJSON.TYPE, MensajesJSON.type.NUEVO_USUARIO.toString());
        respuesta.put(MensajesJSON.VALUES, "Usuario ya existe");

        return respuesta;
    }

    public JSONObject verUsuarios() throws SQLException, IOException {
        return crearMsjListUsuarios(servidor.getDB().obtenerUsuarios(usuario));
    }

    private JSONObject crearMsjListUsuarios(ResultSet rs) throws SQLException, IOException {
        JSONObject respuesta = new JSONObject();
        JSONObject lista = new JSONObject();

        respuesta.put(MensajesJSON.TYPE, MensajesJSON.type.VER_CONECTADOS.toString());
        lista.put(MensajesJSON.LIST, crearListaUsuarios(rs));
        respuesta.put(MensajesJSON.VALUES, lista);

        return respuesta;

    }

    private JSONArray crearListaUsuarios(ResultSet rs) throws SQLException {
        JSONArray respuesta = new JSONArray();
        if (rs != null) {
            do {
                JSONObject usuario = new JSONObject();
                usuario.put(MensajesJSON.USER, rs.getString("nombreUsuario"));
                //String a = actualizar();
                //usuario.put(MensajesJSON.NOM_CANCION, a);
                respuesta.add(usuario);
            } while (rs.next());
        }
        return respuesta;
    }

    //escribir en la base de datos la cancion que está reproduciendo el usuario
    public void saberPistaActual(JSONObject mensaje) throws SQLException, IOException {

        JSONObject valores = (JSONObject) mensaje.get(MensajesJSON.VALUES);
        //nombreCancion = (String) valores.get(MensajesJSON.NOM_CANCION);

        //enviarMsj(crearMsjPista(nombreCancion));
    }

    public void enviarMensajeChat(JSONObject mensaje) {
        JSONObject valores = (JSONObject) mensaje.get(MensajesJSON.VALUES);

        valores.put(MensajesJSON.USER, usuario);
        mensaje.put(MensajesJSON.VALUES, valores);
        mandarA(mensaje);

    }

    private void mandarA(JSONObject mensaje) throws NullPointerException {
        JSONObject valores = (JSONObject) mensaje.get(MensajesJSON.VALUES);
        String cadenaUsuario = (String) valores.get(MensajesJSON.TO);
        valores.remove(MensajesJSON.TO);
        mensaje.put(MensajesJSON.VALUES, valores);

    }

    public void recibirMensajeChat() throws ParseException, IOException {

        JSONObject mensaje = new JSONObject();
        JSONParser parser = new JSONParser();
        String resp = "Nada";
        if (listamensajes.size() > 0) {
            resp = listamensajes.get(0);
            listamensajes.remove(0);
        }
        mensaje = (JSONObject) parser.parse(resp);
        //JSONObject valores = (JSONObject) mensaje.get(MensajesJSON.VALUES);
        enviarMsj(mensaje);

    }

    private JSONObject verCancionesUsuario() throws SQLException, IOException {

        JSONObject valores = (JSONObject) mensaje.get(MensajesJSON.VALUES);
        String nombreUsuario = (String) valores.get(MensajesJSON.USER);

        return (crearMsjListCancionesUsuario(servidor.getDB().obtenerCancionesUsuario(nombreUsuario)));
    }

    private JSONObject crearMsjListCancionesUsuario(ResultSet rs) throws SQLException {
        JSONObject respuesta = new JSONObject();
        JSONObject lista = new JSONObject();

        respuesta.put(MensajesJSON.TYPE, MensajesJSON.type.VER_CANCIONES_USER.toString());
        lista.put(MensajesJSON.LISTC, crearListaCancionesUsuario(rs));
        respuesta.put(MensajesJSON.VALUES, lista);

        return respuesta;

    }

    private JSONObject verListaCanciones() throws IOException, SQLException {

        return crearMsjListCanciones(servidor.getDB().obtenerCanciones());

    }

    private JSONObject crearMsjListCanciones(ResultSet rs) throws SQLException {
        JSONObject respuesta = new JSONObject();
        JSONObject lista = new JSONObject();

        respuesta.put(MensajesJSON.TYPE, MensajesJSON.type.VER_CANCIONES.toString());
        lista.put(MensajesJSON.LIST, crearListaCanciones(rs));
        respuesta.put(MensajesJSON.VALUES, lista);

        return respuesta;

    }

    public JSONArray crearListaCanciones(ResultSet rs) throws SQLException {
        JSONArray respuesta = new JSONArray();
        if (rs != null) {
            do {
                JSONObject cancion = new JSONObject();
                cancion.put(MensajesJSON.NOM_CANCION, rs.getString("nombreCancion"));
                cancion.put(MensajesJSON.RUTA_CANCION, rs.getString("ruta"));
                respuesta.add(cancion);
            } while (rs.next());
        }
        return respuesta;
    }

    public JSONArray crearListaCancionesUsuario(ResultSet rs) throws SQLException {
        JSONArray respuesta = new JSONArray();
        if (rs != null) {
            do {
                JSONObject cancion = new JSONObject();
                cancion.put(MensajesJSON.USER, rs.getString("nombreUsuario"));
                cancion.put(MensajesJSON.NOM_CANCION, rs.getString("nombreCancion"));
                respuesta.add(cancion);
            } while (rs.next());
        }
        return respuesta;
    }

    public String cerrarSesion() {
        return null;
    }

    public void enviarMsj(JSONObject mensaje) throws IOException {
        out.writeUTF(mensaje.toString());
        out.flush();
    }

    private JSONObject crearMsjLoginUsuario(boolean exito) {
        JSONObject respuesta = new JSONObject();
        //respuesta.put(MensajesJSON.TYPE, MensajesJSON.type.LOGIN.toString());
        respuesta.put(MensajesJSON.VALUES, true);

        return respuesta;
    }

    private JSONObject crearMsjELoginUsuario(boolean exito) {
        JSONObject respuesta = new JSONObject();
        // respuesta.put(MensajesJSON.TYPE, MensajesJSON.type.LOGIN.toString());
        respuesta.put(MensajesJSON.VALUES, "Usuario ya logueado");

        return respuesta;
    }

    private JSONObject crearMsjSubirCancion(boolean exito) {
        JSONObject respuesta = new JSONObject();
        //respuesta.put(MensajesJSON.TYPE, MensajesJSON.type.LOGIN.toString());
        respuesta.put(MensajesJSON.VALUES, true);

        return respuesta;
    }

    private JSONObject crearMsjESubirCancion(boolean exito) {
        JSONObject respuesta = new JSONObject();
        // respuesta.put(MensajesJSON.TYPE, MensajesJSON.type.LOGIN.toString());
        respuesta.put(MensajesJSON.VALUES, "Usuario ya logueado");

        return respuesta;
    }

    private JSONObject crearMsjDescargarCancion(boolean exito) {
        JSONObject respuesta = new JSONObject();
        //respuesta.put(MensajesJSON.TYPE, MensajesJSON.type.LOGIN.toString());
        respuesta.put(MensajesJSON.VALUES, "listo");

        return respuesta;
    }

    private JSONObject crearMsjEDescargarCancion(boolean exito) {
        JSONObject respuesta = new JSONObject();
        // respuesta.put(MensajesJSON.TYPE, MensajesJSON.type.LOGIN.toString());
        respuesta.put(MensajesJSON.VALUES, "Cancion ya añadida");

        return respuesta;
    }

    private JSONObject crearMsjPista(String pista) {
        JSONObject respuesta = new JSONObject();

        //  respuesta.put(MensajesJSON.TYPE, MensajesJSON.type.NUEVO_USUARIO.toString());
        respuesta.put(MensajesJSON.VALUES, pista);

        return respuesta;
    }

    public JSONObject salir(JSONObject mensaje) throws SQLException, IOException {

        JSONObject valores = (JSONObject) mensaje.get(MensajesJSON.VALUES);
        String nombreUsuario = (String) valores.get(MensajesJSON.USER);

        servidor.getDB().desconectarUsuario(nombreUsuario);
        JSONObject respuesta = new JSONObject();
        respuesta.put(MensajesJSON.VALUES, "Salir");

        return (respuesta);

    }

}
