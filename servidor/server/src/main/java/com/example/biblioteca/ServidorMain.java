package com.example.biblioteca;

import com.example.biblioteca.db.BibliotecaDao;
import com.example.biblioteca.db.ConexionSqlite;
import com.example.biblioteca.grpc.BibliotecaServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ServidorMain {
  public static void main(String[] args) throws Exception {
    int puerto = (args.length > 0) ? Integer.parseInt(args[0]) : 50051;

    // BD local del servidor (se crea automáticamente)
    String rutaDb = "db/biblioteca.db";
    var conexion = new ConexionSqlite(rutaDb);
    conexion.inicializar("db/schema.sql", "db/seed.sql");

    var dao = new BibliotecaDao(conexion);

    Server server = ServerBuilder.forPort(puerto)
        .addService(new BibliotecaServiceImpl(dao))
        .build()
        .start();

    System.out.println("Servidor gRPC listo en puerto " + puerto);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("\nCerrando servidor...");
      server.shutdown();
    }));

    server.awaitTermination();
  }
}
