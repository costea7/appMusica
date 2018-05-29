/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reproductormúsica;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 *
 * @author Iulian
 */
public class GestorBD {

    private static GestorBD instancia = null;
    private final int MAX_RESULT_FOR_QUERY = 50;

    /* Credenciales para conectar la BD */
    private final String ADMINDB = "u681177";
    private final String PASS = "u681177";
    private static final String NOMBRE_BD = "u681177";
    private static final String DIRECCION_BD = "web-ter.unizar.es:3306";
    private Connection conexionDB;
    private final String URL_DB = "jdbc:mysql://" + DIRECCION_BD + "/" + NOMBRE_BD;

    private final String queryGetUsers = "SELECT * FROM `usuario` WHERE `estado`='conectado' AND `nombreUsuario`!='%s' ORDER BY RAND() LIMIT %d";
    private final String queryRegiter = "INSERT INTO `usuario`(`nombreUsuario`, `Password`, `estado`)"
            + " VALUES ('%s','%s', '%s');";
    private final String queryCheckPass = "SELECT * FROM `usuario` WHERE `nombreUsuario`='%s' AND `Password`='%s'";
    private final String queryStatus = "SELECT * FROM `usuario` WHERE `nombreUsuario`='%s' AND `estado`='desconectado'";
    private final String queryUpdateStatus = "UPDATE `usuario` SET `estado`='%s' WHERE `nombreUsuario` = '%s'";
    private final String queryCanciones = "SELECT * FROM `cancion`";
    private final String queryCancionesUsuario = "SELECT `nombreCancion` FROM `usuario-cancion` WHERE `nombreUsuario` = '%s' ORDER BY RAND() LIMIT %d";
    private final String queryUsers = "SELECT * FROM `usuario` WHERE `estado`!='desconectado'";
    private final String querySubirCancion = "INSERT INTO `cancion`(`nombreCancion`, `ruta`)"
            + " VALUES ('%s','%s');";
    private final String queryDescargarCancion = "INSERT INTO `usuario-cancion`(`nombreUsuario`, `nombreCancion`)"
            + " VALUES ('%s','%s');";

    public enum EstadoUsuario {
        conectado, desconectado
    };

    private GestorBD() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conexionDB = DriverManager.getConnection(URL_DB, ADMINDB, PASS);
            conexionDesconectar();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Método Singleton
     *
     * @return
     */
    public static GestorBD getInstancia() {
        if (instancia == null) {
            instancia = new GestorBD();
        }

        return instancia;
    }

    /**
     * 
     */
    private void conexionDesconectar() throws SQLException {
        ArrayList lista = new ArrayList();

        Statement s = this.conexionDB.createStatement();
        ResultSet rs = s.executeQuery(queryUsers);

        while (rs.next()) {
            lista.add(rs.getString("nombreUsuario"));
        }

        while (!lista.isEmpty()) {
            s.executeUpdate(String.format(queryUpdateStatus, EstadoUsuario.desconectado, (String) lista.get(0)));
            lista.remove(0);
        }
    }

    public ResultSet obtenerUsuarios(String usuario) {
        Statement s;
        ResultSet respuesta = null;

        try {
            s = conexionDB.createStatement();
            respuesta = s.executeQuery(String.format(queryGetUsers, usuario, MAX_RESULT_FOR_QUERY));

            if (!respuesta.next()) {
                respuesta = null;
            }
        } catch (SQLException ex) {
            respuesta = null;
            ex.printStackTrace();

        } finally {
            return respuesta;
        }
    }

    public ResultSet obtenerCanciones() {
        Statement s;
        ResultSet respuesta = null;

        try {
            s = conexionDB.createStatement();
            respuesta = s.executeQuery(String.format(queryCanciones, MAX_RESULT_FOR_QUERY));

            if (!respuesta.next()) {
                respuesta = null;
            }
        } catch (SQLException ex) {
            respuesta = null;
            ex.printStackTrace();

        } finally {
            return respuesta;
        }
    }

    public ResultSet obtenerCancionesUsuario(String nombreUsuario) {
        Statement s;
        ResultSet respuesta = null;

        try {
            s = conexionDB.createStatement();
            respuesta = s.executeQuery(String.format(queryCancionesUsuario, nombreUsuario, MAX_RESULT_FOR_QUERY));

            if (!respuesta.next()) {
                respuesta = null;
            }
        } catch (SQLException ex) {
            respuesta = null;
            ex.printStackTrace();

        } finally {
            return respuesta;
        }
    }

    public boolean registrarUsuario(String nombreUsuario, String pass) {

        boolean respuesta = false;
        int i;
        Statement s;

        try {
            s = this.conexionDB.createStatement();
            i = s.executeUpdate(String.format(queryRegiter, nombreUsuario, pass, EstadoUsuario.desconectado));

            respuesta = i != 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            if (ex instanceof SQLIntegrityConstraintViolationException) {
                ex.printStackTrace();
                return respuesta;
            } else {
                ex.printStackTrace();
                // this.exceptionHandler(ex);
            }
        } finally {
            return respuesta;
        }
    }

    /**
     * public int comprobarAutentificacion(String usuario, String pass)
     *
     */
    public boolean comprobarAutentificacion(String nombreUsuario, String pass) {
        Statement s;
        ResultSet rs;

        try {
            s = this.conexionDB.createStatement();
            rs = s.executeQuery(String.format(queryCheckPass, nombreUsuario, pass));

            if (rs.next()) {
                if (this.iniciarSesion(nombreUsuario)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean iniciarSesion(String nombreUsuario) throws SQLException {
        boolean respuesta = false;
        Statement s;
        ResultSet rs = null;
        try {
            s = conexionDB.createStatement();
            rs = s.executeQuery(String.format(queryStatus, nombreUsuario));

            if (!rs.next()) {
                respuesta = false;

            } else if (s.executeUpdate(String.format(queryUpdateStatus, EstadoUsuario.conectado, nombreUsuario)) != 0) {//<---
                respuesta = true;

            }
        } catch (SQLException ex) {

            ex.printStackTrace();
        }

        System.out.println("respuesta " + respuesta);
        return respuesta;
    }

    public boolean subirCancion(String nombreCancion, String ruta) {

        boolean respuesta = false;
        int i;
        Statement s;

        try {
            s = this.conexionDB.createStatement();
            i = s.executeUpdate(String.format(querySubirCancion, nombreCancion, ruta));

            respuesta = i != 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            if (ex instanceof SQLIntegrityConstraintViolationException) {
                ex.printStackTrace();
                return respuesta;
            } else {
                ex.printStackTrace();
            }
        } finally {
            return respuesta;
        }
    }

    public boolean descargarCancion(String nombreUsuario, String nombreCancion) {

        boolean respuesta = false;
        int i;
        Statement s;

        try {
            s = this.conexionDB.createStatement();
            i = s.executeUpdate(String.format(queryDescargarCancion, nombreUsuario, nombreCancion));

            respuesta = i != 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            if (ex instanceof SQLIntegrityConstraintViolationException) {
                ex.printStackTrace();
                return respuesta;
            } else {
                ex.printStackTrace();
            }
        } finally {
            return respuesta;
        }
    }

}
