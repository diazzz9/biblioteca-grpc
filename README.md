Sistema de Biblioteca con gRPC

Taller de Sistemas Distribuidos — Febrero 2026  
Hecho por: Jose Guerrero - Samuel Giraldo - Marianne Coy - Daniel Diaz

Este proyecto es un sistema de préstamos de biblioteca. Tiene un servidor en Java con gRPC y una base de datos SQLite, y un cliente con interfaz gráfica hecha en Java Swing.

---

Cómo está organizado

```
proyecto/
├── server/              * El servidor
│   ├── db/
│   │   └── biblioteca.db
│   └── src/main/resources/
│       ├── schema.sql
│       └── seed.sql
│
└── cliente/             * El cliente con interfaz
    └── app/
        └── src/main/java/com/example/cliente/
            ├── BibliotecaGUI.java
            └── ClienteMain.java




Cómo correr el servidor

Esto va en la PC que hace de servidor.

   bash
cd server
mvn clean package
mvn exec:java -Dexec.args="50051"
```

La base de datos se crea sola la primera vez. Queda escuchando en el puerto 50051.

---

Cómo correr el cliente

Esto va en la otra PC, que tenga interfaz gráfica.

```bash
cd cliente/app
mvn compile
mvn exec:java
```

Cuando abra la ventana, poner la IP del servidor y el puerto 50051, luego clic en Conectar.

---

Qué puede hacer

- **Consultar libro por ISBN** — dice si existe y cuántos ejemplares hay
- **Prestar por ISBN** — registra el préstamo y da la fecha de devolución (7 días)
- **Prestar por Título** — lo mismo pero buscando por nombre del libro
- **Devolver libro** — registra la devolución y actualiza los ejemplares

Todo funciona de forma síncrona.

---

Libros que tiene el sistema

| ISBN 		| Título |
|------		|--------|
| 9780307474278 | — |
| 9788437604947 | — |
| 9788466333978 | — |
| 9780060883287 | — |
| 9789500721507 | — |

---

Tecnologías usadas

- Java 17
- Maven
- gRPC y Protocol Buffers
- SQLite
- Java Swing

---

Para probar con dos clientes

Abrir dos terminales en la PC cliente y correr `mvn exec:java` en cada una. Las dos se conectan al mismo servidor.
