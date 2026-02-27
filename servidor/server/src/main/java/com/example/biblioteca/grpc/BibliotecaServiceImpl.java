package com.example.biblioteca.grpc;

import com.example.biblioteca.db.BibliotecaDao;
import io.grpc.stub.StreamObserver;

import library.BibliotecaServiceGrpc;
import library.ConsultarReq;
import library.ConsultarRes;
import library.DevolverReq;
import library.DevolverRes;
import library.PrestarPorIsbnReq;
import library.PrestarPorTituloReq;
import library.PrestarRes;

public class BibliotecaServiceImpl extends BibliotecaServiceGrpc.BibliotecaServiceImplBase {

  private final BibliotecaDao dao;

  public BibliotecaServiceImpl(BibliotecaDao dao) {
    this.dao = dao;
  }

  @Override
  public void consultar(ConsultarReq req, StreamObserver<ConsultarRes> obs) {
    try {
      String isbn = req.getIsbn().trim();
      var opt = dao.buscarPorIsbn(isbn);

      if (opt.isEmpty()) {
        obs.onNext(ConsultarRes.newBuilder()
            .setExiste(false)
            .setMensaje("No existe en la biblioteca")
            .setIsbn(isbn)
            .build());
        obs.onCompleted();
        return;
      }

      var l = opt.get();
      obs.onNext(ConsultarRes.newBuilder()
          .setExiste(true)
          .setMensaje("Existe")
          .setIsbn(l.isbn())
          .setTitulo(l.titulo())
          .setTotalEjemplares(l.total())
          .setEjemplaresDisponibles(l.disponibles())
          .build());
      obs.onCompleted();

    } catch (Exception e) {
      obs.onError(e);
    }
  }

  @Override
  public void prestarPorIsbn(PrestarPorIsbnReq req, StreamObserver<PrestarRes> obs) {
    try {
      String isbn = req.getIsbn().trim();
      String prestatario = req.getPrestatario().trim();

      var r = dao.prestar(isbn, prestatario);

      PrestarRes.Builder b = PrestarRes.newBuilder()
          .setIsbn(isbn);

      if (r.noExiste) {
        obs.onNext(b.setOk(false).setMensaje("ISBN no encontrado").build());
        obs.onCompleted();
        return;
      }

      if (!r.ok) {
        obs.onNext(b.setOk(false)
            .setMensaje(r.mensaje)
            .setTitulo(r.titulo == null ? "" : r.titulo)
            .build());
        obs.onCompleted();
        return;
      }

      obs.onNext(b.setOk(true)
          .setMensaje("Préstamo exitoso. Vence en 7 días.")
          .setTitulo(r.titulo)
          .setFechaDevolucionIso(r.vence.toString())
          .build());
      obs.onCompleted();

    } catch (Exception e) {
      obs.onError(e);
    }
  }

  @Override
  public void prestarPorTitulo(PrestarPorTituloReq req, StreamObserver<PrestarRes> obs) {
    try {
      String titulo = req.getTitulo().trim();
      String prestatario = req.getPrestatario().trim();

      var opt = dao.buscarPorTitulo(titulo);
      if (opt.isEmpty()) {
        obs.onNext(PrestarRes.newBuilder()
            .setOk(false)
            .setMensaje("Título no encontrado")
            .setTitulo(titulo)
            .build());
        obs.onCompleted();
        return;
      }

      var libro = opt.get();
      var r = dao.prestar(libro.isbn(), prestatario);

      if (!r.ok) {
        obs.onNext(PrestarRes.newBuilder()
            .setOk(false)
            .setMensaje(r.mensaje)
            .setIsbn(libro.isbn())
            .setTitulo(libro.titulo())
            .build());
        obs.onCompleted();
        return;
      }

      obs.onNext(PrestarRes.newBuilder()
          .setOk(true)
          .setMensaje("Préstamo exitoso. Vence en 7 días.")
          .setIsbn(libro.isbn())
          .setTitulo(libro.titulo())
          .setFechaDevolucionIso(r.vence.toString())
          .build());
      obs.onCompleted();

    } catch (Exception e) {
      obs.onError(e);
    }
  }

  @Override
  public void devolver(DevolverReq req, StreamObserver<DevolverRes> obs) {
    try {
      String isbn = req.getIsbn().trim();
      var r = dao.devolver(isbn);

      if (!r.ok) {
        obs.onNext(DevolverRes.newBuilder()
            .setOk(false)
            .setMensaje(r.mensaje)
            .build());
        obs.onCompleted();
        return;
      }

      obs.onNext(DevolverRes.newBuilder()
          .setOk(true)
          .setMensaje("Libro devuelto. Gracias.")
          .setEjemplaresDisponibles(r.disponibles)
          .build());
      obs.onCompleted();

    } catch (Exception e) {
      obs.onError(e);
    }
  }
}
