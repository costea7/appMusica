/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reproductormúsica;

import archivos.Clientes_Server.MensajeDameFichero;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.io.ObjectInputStream;
import java.io.PrintWriter;
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

/**
 *
 * @author Iulian
 */
public class HiloEjecucion extends Thread implements Runnable {

    private final Socket socket;
    private final Servidor servidor;
    private DataInputStream in;
    private DataOutputStream out;

    private String usuario;

    public HiloEjecucion(Servidor servidor, Socket cliente) throws IOException {
        socket = cliente;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        this.servidor = servidor;
    }

    @Override
    public void run() {
        JSONObject json;
        JSONParser parser = new JSONParser();
        String cadena = "";
        try {
            while (!cadena.equals("Salir")) {

                cadena = in.readUTF();
                System.out.println(cadena);
                json = (JSONObject) parser.parse(cadena);
                this.gestionReproductor(json);

            }
        } catch (IOException ex) {
            Logger.getLogger(HiloEjecucion.class
                    .getName()).log(Level.SEVERE, null, ex);

        } catch (ParseException ex) {
            Logger.getLogger(HiloEjecucion.class
                    .getName()).log(Level.SEVERE, null, ex);

        } catch (ClassNotFoundException ex) {
            Logger.getLogger(HiloEjecucion.class
                    .getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(HiloEjecucion.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void gestionReproductor(JSONObject mensaje) throws IOException, ClassNotFoundException, SQLException {
        switch (MensajesJSON.type.valueOf((String) mensaje.get(MensajesJSON.TYPE))) {
            case LOGIN:
                loginUsuario(mensaje);
                break;
            case REGISTRO:
                registrarUsuario(mensaje);
                break;
            case CERRAR_SESION:
                cerrarSesion();
                break;
            case VER_CONECTADOS:
                verUsuarios();
                break;
            case ENVIAR_MENSAJE:
                enviarMensajeChat(mensaje);
                break;
            case RECIBIR_MENSAJE:
                recibirMensajeChat();
                break;
            case VER_CANCIONES:
                verListaCanciones();
                break;
            case VER_CANCIONES_USER:
                verCancionesUsuario(mensaje);
                break;
            case DESCARGAR_CANCION:
                descargarCancion(mensaje, socket);
                break;
            case SUBIR_CANCION:
                subirCancion(mensaje, socket);
                break;
            case PISTA_ACTUAL:
                saberPistaActual(mensaje);
                break;
        }
    }

    public void registrarUsuario(JSONObject mensaje) throws IOException {
        JSONObject valores = (JSONObject) mensaje.get(MensajesJSON.VALUES);

        String usuario = (String) valores.get(MensajesJSON.USER);
        String pass = (String) valores.get(MensajesJSON.PASS);
        System.out.println("confirmación registro: " + usuario + " - " + pass);

        boolean respuesta = servidor.getDB().registrarUsuario(usuario, pass);

        if (respuesta) {
            enviarMsj(crearMsjNuevoUsuario(respuesta));
        } else {
            enviarMsj(crearMsjENuevoUsuario(respuesta));

        }
    }

    public void subirCancion(JSONObject mensaje, Socket cliente) throws IOException, ClassNotFoundException {

        JSONObject valores = (JSONObject) mensaje.get(MensajesJSON.VALUES);
        String nombreCancion = (String) valores.get(MensajesJSON.NOM_CANCION);
        String ruta = (String) valores.get(MensajesJSON.RUTA_CANCION);

        boolean respuesta = servidor.getDB().subirCancion(nombreCancion, ruta);

        if (respuesta) {
            enviarMsj(crearMsjSubirCancion(respuesta));
        } else {
            enviarMsj(crearMsjESubirCancion(respuesta));

        }

        ObjectInputStream ois = new ObjectInputStream(cliente.getInputStream());
        Object cancion = ois.readObject();

        if (cancion instanceof MensajeTomaFichero) {

            hilorecibe hr = new hilorecibe(((MensajeTomaFichero) cancion).nombreFichero, ois);
            hr.start();
        }

    }

    private void descargarCancion(JSONObject mensaje, Socket cliente) throws IOException, IOException, ClassNotFoundException {

        JSONObject valores = (JSONObject) mensaje.get(MensajesJSON.VALUES);
        String nombreUsuario = (String) valores.get(MensajesJSON.USER);
        String nombreCancion = (String) valores.get(MensajesJSON.NOM_CANCION);

        boolean respuesta = servidor.getDB().descargarCancion(nombreUsuario, nombreCancion);

        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(cliente.getInputStream());
            Object cancion = ois.readObject();
    
            hiloenvio hiloenv = new hiloenvio(((MensajeDameFichero) cancion).nombreFichero, new ObjectOutputStream(cliente.getOutputStream()));
            hiloenv.start();
        } catch (IOException | ClassNotFoundException ex) {
          
        }

        if (respuesta) {
            enviarMsj(crearMsjDescargarCancion(respuesta));
        } else {
            enviarMsj(crearMsjEDescargarCancion(respuesta));
        }

    }

    public void loginUsuario(JSONObject mensaje) throws IOException {
        JSONObject valores = (JSONObject) mensaje.get(MensajesJSON.VALUES);

        String usuario = (String) valores.get(MensajesJSON.USER);
        String pass = (String) valores.get(MensajesJSON.PASS);
        System.out.println("confirmación login: " + usuario + " - " + pass);

        boolean respuesta = servidor.getDB().comprobarAutentificacion(usuario, pass);

        if (respuesta) {

            enviarMsj(crearMsjLoginUsuario(respuesta));
        } else {

            enviarMsj(crearMsjELoginUsuario(respuesta));
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

    public void verUsuarios() throws SQLException, IOException {
        enviarMsj(crearMsjListUsuarios(servidor.getDB().obtenerUsuarios(usuario)));
    }

    private JSONObject crearMsjListUsuarios(ResultSet rs) throws SQLException {
        JSONObject respuesta = new JSONObject();
        JSONObject lista = new JSONObject();

        //respuesta.put(MensajesJSON.TYPE, MensajesJSON.type.VER_CONECTADOS.toString());
        lista.put(MensajesJSON.LIST, crearListaUsuarios(rs));
        respuesta.put(MensajesJSON.VALUES, lista.values());

        return respuesta;

    }

    private JSONArray crearListaUsuarios(ResultSet rs) throws SQLException {
        JSONArray respuesta = new JSONArray();
        if (rs != null) {
            do {
                JSONObject usuario = new JSONObject();
                usuario.put(MensajesJSON.USER, rs.getString("nombreUsuario"));
                respuesta.add(usuario);
            } while (rs.next());
        }
        return respuesta;
    }

    //escribir en la base de datos la cancion que está reproduciendo el usuario
    public void saberPistaActual(JSONObject mensaje) {

        JSONObject valores = (JSONObject) mensaje.get(MensajesJSON.VALUES);
        String pistaActual = (String) valores.get(MensajesJSON.RUTA_CANCION);

        //servidor.getDB().pistaActual();
    }

    public void enviarMensajeChat(JSONObject mensaje) {
        JSONObject valores = (JSONObject) mensaje.get(MensajesJSON.VALUES);

        valores.put(MensajesJSON.USER, usuario);
        mensaje.put(MensajesJSON.VALUES, valores);

        sendTo(mensaje);

    }

    private void sendTo(JSONObject mensaje) throws NullPointerException {
        JSONObject valores = (JSONObject) mensaje.get(MensajesJSON.VALUES);
        String cadenaUsuario = (String) valores.get(MensajesJSON.TO);
        valores.remove(MensajesJSON.TO);
        mensaje.put(MensajesJSON.VALUES, valores);

        //servidor.getObjectStore(cadenaUsuario).enviarMsj(mensaje);
    }

    public void recibirMensajeChat() {
        //servidor.getDB().recuperarMensajes();
    }

    private void verListaCanciones() throws IOException {
        try {
            enviarMsj(crearMsjListCanciones(servidor.getDB().obtenerCanciones()));
        } catch (SQLException ex) {
            Logger.getLogger(HiloEjecucion.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void verCancionesUsuario(JSONObject mensaje) throws SQLException, IOException {

        JSONObject valores = (JSONObject) mensaje.get(MensajesJSON.VALUES);
        String nombreUsuario = (String) valores.get(MensajesJSON.USER);

        enviarMsj(crearMsjListCancionesUsuario(servidor.getDB().obtenerCancionesUsuario(nombreUsuario)));
    }

    private JSONObject crearMsjListCanciones(ResultSet rs) throws SQLException {
        JSONObject respuesta = new JSONObject();
        JSONObject lista = new JSONObject();

        respuesta.put(MensajesJSON.TYPE, MensajesJSON.type.VER_CANCIONES.toString());
        lista.put(MensajesJSON.LIST, crearListaCanciones(rs));
        respuesta.put(MensajesJSON.VALUES, lista);

        return respuesta;

    }

    private JSONObject crearMsjListCancionesUsuario(ResultSet rs) throws SQLException {
        JSONObject respuesta = new JSONObject();
        JSONObject lista = new JSONObject();

        respuesta.put(MensajesJSON.TYPE, MensajesJSON.type.VER_CANCIONES_USER.toString());
        lista.put(MensajesJSON.LISTC, crearListaCancionesUsuario(rs));
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
}
