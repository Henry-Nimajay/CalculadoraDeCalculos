
package gestorjugadores;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {
    String bd="videojuegos";
    String url="jdbc:mysql://localhost:3306/";
    String user="root";
    String password="admin";
    String driver="com.mysql.cj.jdbc.Driver";
    Connection cx;

    public Conexion(String bd) {
        this.bd=bd;
    }
    
    public Connection conectar() {
        try {
            Class.forName(driver);
            cx=DriverManager.getConnection(url+bd, user, password);
            System.out.println("Se Conecto a BD"+bd);
        } catch (ClassNotFoundException | SQLException ex) {
            System.out.println("No se Conecto a BD"+bd);
            System.getLogger(Conexion.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return cx;
    }
    
    public void desconectar() {
        try {
            cx.close();
        } catch (SQLException ex) {
            System.getLogger(Conexion.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }
 
    public static void main(String[] args) {
        Conexion conexion=new Conexion("videojuegos1");
        conexion.conectar();
    }
    
}
