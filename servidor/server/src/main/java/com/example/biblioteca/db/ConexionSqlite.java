package com.example.biblioteca.db;

import java.nio.file.*;
import java.sql.*;

public class ConexionSqlite {
  private final String url;

  public ConexionSqlite(String rutaDb) {
    this.url = "jdbc:sqlite:" + rutaDb;
  }

  public Connection abrir() throws SQLException {
    return DriverManager.getConnection(url);
  }

  public void inicializar(String schemaPath, String seedPath) throws Exception {
    Path ruta = Paths.get(extraerRutaDb()).toAbsolutePath();
    if (ruta.getParent() != null) Files.createDirectories(ruta.getParent());

    try (Connection c = abrir()) {
      c.setAutoCommit(false);
      ejecutarSql(c, schemaPath);
      ejecutarSql(c, seedPath);
      c.commit();
    }
  }

  private void ejecutarSql(Connection c, String path) throws Exception {
    String sql = Files.readString(Paths.get(path));
    try (Statement st = c.createStatement()) {
      st.executeUpdate(sql);
    }
  }

  private String extraerRutaDb() {
    return url.replace("jdbc:sqlite:", "");
  }
}
