package com.example.biblioteca.db;

import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;

public class BibliotecaDao {
  private final ConexionSqlite db;

  public record Libro(String isbn, String titulo, int total, int disponibles) {}

  public static class PrestamoResultado {
    public final boolean ok;
    public final boolean noExiste;
    public final String titulo;
    public final LocalDate vence;
    public final String mensaje;

    private PrestamoResultado(boolean ok, boolean noExiste, String titulo, LocalDate vence, String mensaje) {
      this.ok = ok;
      this.noExiste = noExiste;
      this.titulo = titulo;
      this.vence = vence;
      this.mensaje = mensaje;
    }

    public static PrestamoResultado ok(String titulo, LocalDate vence) {
      return new PrestamoResultado(true, false, titulo, vence, "Préstamo exitoso");
    }

    public static PrestamoResultado noExiste() {
      return new PrestamoResultado(false, true, "", null, "ISBN no encontrado");
    }

    public static PrestamoResultado noDisponibles(String titulo) {
      return new PrestamoResultado(false, false, titulo, null, "No hay ejemplares disponibles");
    }
  }

  public static class DevolucionResultado {
    public final boolean ok;
    public final String mensaje;
    public final int disponibles;

    private DevolucionResultado(boolean ok, String mensaje, int disponibles) {
      this.ok = ok;
      this.mensaje = mensaje;
      this.disponibles = disponibles;
    }

    public static DevolucionResultado ok(int disponibles) {
      return new DevolucionResultado(true, "Devolución realizada", disponibles);
    }

    public static DevolucionResultado noHayPrestamo() {
      return new DevolucionResultado(false, "No hay préstamo activo para ese ISBN", -1);
    }

    public static DevolucionResultado libroNoExiste() {
      return new DevolucionResultado(false, "El libro no existe", -1);
    }
  }

  public BibliotecaDao(ConexionSqlite db) {
    this.db = db;
  }

  public Optional<Libro> buscarPorIsbn(String isbn) throws SQLException {
    String sql = "SELECT isbn, titulo, total_ejemplares, ejemplares_disponibles FROM libros WHERE isbn=?";
    try (Connection c = db.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, isbn);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return Optional.empty();
        return Optional.of(new Libro(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4)));
      }
    }
  }

  public Optional<Libro> buscarPorTitulo(String titulo) throws SQLException {
    String sql = "SELECT isbn, titulo, total_ejemplares, ejemplares_disponibles FROM libros " +
        "WHERE lower(titulo)=lower(?) LIMIT 1";
    try (Connection c = db.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, titulo);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return Optional.empty();
        return Optional.of(new Libro(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4)));
      }
    }
  }

  public PrestamoResultado prestar(String isbn, String prestatario) throws SQLException {
    try (Connection c = db.abrir()) {
      c.setAutoCommit(false);

      Libro libro;
      try (PreparedStatement ps = c.prepareStatement(
          "SELECT isbn,titulo,total_ejemplares,ejemplares_disponibles FROM libros WHERE isbn=?")) {
        ps.setString(1, isbn);
        try (ResultSet rs = ps.executeQuery()) {
          if (!rs.next()) { c.rollback(); return PrestamoResultado.noExiste(); }
          libro = new Libro(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4));
        }
      }

      if (libro.disponibles() <= 0) { c.rollback(); return PrestamoResultado.noDisponibles(libro.titulo()); }

      try (PreparedStatement up = c.prepareStatement(
          "UPDATE libros SET ejemplares_disponibles = ejemplares_disponibles - 1 " +
              "WHERE isbn=? AND ejemplares_disponibles > 0")) {
        up.setString(1, isbn);
        if (up.executeUpdate() == 0) { c.rollback(); return PrestamoResultado.noDisponibles(libro.titulo()); }
      }

      LocalDate hoy = LocalDate.now();
      LocalDate vence = hoy.plusDays(7);

      try (PreparedStatement ins = c.prepareStatement(
          "INSERT INTO prestamos(isbn, prestatario, fecha_prestamo, fecha_devolucion, devuelto) " +
              "VALUES(?,?,?,?,0)")) {
        ins.setString(1, isbn);
        ins.setString(2, prestatario);
        ins.setString(3, hoy.toString());
        ins.setString(4, vence.toString());
        ins.executeUpdate();
      }

      c.commit();
      return PrestamoResultado.ok(libro.titulo(), vence);
    }
  }

  public DevolucionResultado devolver(String isbn) throws SQLException {
    try (Connection c = db.abrir()) {
      c.setAutoCommit(false);

      Integer idPrestamo = null;
      try (PreparedStatement ps = c.prepareStatement(
          "SELECT id_prestamo FROM prestamos WHERE isbn=? AND devuelto=0 ORDER BY id_prestamo ASC LIMIT 1")) {
        ps.setString(1, isbn);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) idPrestamo = rs.getInt(1);
        }
      }

      if (idPrestamo == null) { c.rollback(); return DevolucionResultado.noHayPrestamo(); }

      try (PreparedStatement up = c.prepareStatement(
          "UPDATE prestamos SET devuelto=1, fecha_entrega=? WHERE id_prestamo=?")) {
        up.setString(1, LocalDate.now().toString());
        up.setInt(2, idPrestamo);
        up.executeUpdate();
      }

      try (PreparedStatement up2 = c.prepareStatement(
          "UPDATE libros SET ejemplares_disponibles = MIN(total_ejemplares, ejemplares_disponibles + 1) WHERE isbn=?")) {
        up2.setString(1, isbn);
        if (up2.executeUpdate() == 0) { c.rollback(); return DevolucionResultado.libroNoExiste(); }
      }

      int disponibles;
      try (PreparedStatement ps = c.prepareStatement("SELECT ejemplares_disponibles FROM libros WHERE isbn=?")) {
        ps.setString(1, isbn);
        try (ResultSet rs = ps.executeQuery()) {
          rs.next();
          disponibles = rs.getInt(1);
        }
      }

      c.commit();
      return DevolucionResultado.ok(disponibles);
    }
  }
}
