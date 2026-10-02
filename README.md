# gRPC Library System

A distributed library loan system built for the **Distributed Systems** course at Pontificia Universidad Javeriana (February 2026). A Java **gRPC** server with **SQLite** persistence exposes the library's operations, and a **Java Swing** desktop client calls them remotely — the client and server can run on different machines.

<a href="https://drive.google.com/file/d/111_ZIPO7beguF9903jV2GrLotwukdgLI/view?usp=sharing"><img src="docs/demo.svg" width="340" alt="Watch the demo video"></a>

**Authors:** José Guerrero · Samuel Giraldo · Marianne Coy · Daniel Díaz

---

## Features

| Operation | RPC | What it does |
|---|---|---|
| Look up a book | `Consultar` | Checks whether an ISBN exists and how many copies are available |
| Borrow by ISBN | `PrestarPorIsbn` | Registers a loan and returns the due date (7 days) |
| Borrow by title | `PrestarPorTitulo` | Same, searching by book title |
| Return a book | `Devolver` | Registers the return and updates available copies |

All calls are **synchronous** (unary RPCs) defined in [`library.proto`](servidor/server/src/main/proto/library.proto). Multiple clients can connect to the same server at once.

## Architecture

```
┌──────────────────────┐         gRPC / HTTP2          ┌───────────────────────────┐
│  Swing desktop client │  ───────────────────────────▶ │  gRPC server (port 50051) │
│  BibliotecaGUI        │  ◀───────────────────────────  │  BibliotecaServiceImpl    │
└──────────────────────┘        Protocol Buffers        │            │              │
                                                        │      BibliotecaDao        │
                                                        │            ▼              │
                                                        │   SQLite (biblioteca.db)  │
                                                        └───────────────────────────┘
```

```
.
├── servidor/server/                     # gRPC server
│   ├── db/schema.sql · seed.sql         # tables (libros, prestamos) + sample data
│   └── src/main/
│       ├── proto/library.proto          # service contract
│       └── java/com/example/biblioteca/
│           ├── ServidorMain.java        # starts the server
│           ├── grpc/BibliotecaServiceImpl.java
│           └── db/ConexionSqlite.java · BibliotecaDao.java
└── cliente/app/                         # Swing client
    └── src/main/java/com/example/cliente/
        ├── BibliotecaGUI.java
        └── ClienteMain.java
```

## Tech stack

Java 17 · Maven · gRPC 1.64 · Protocol Buffers · SQLite · Java Swing

## Running it

**1. Server** (machine acting as server):

```bash
cd servidor/server
mvn clean package
mvn exec:java -Dexec.args="50051"
```

The database is created automatically on first run and the server listens on port `50051`.

**2. Client** (any machine with a graphical interface):

```bash
cd cliente/app
mvn compile
mvn exec:java
```

When the window opens, enter the server's IP address and port `50051`, then click **Conectar**. To test concurrency, open a second terminal and start another client.

## Sample data

| ISBN | Title |
|---|---|
| 9780307474278 | Cien años de soledad |
| 9788437604947 | El amor en los tiempos del cólera |
| 9788466333978 | La sombra del viento |
| 9780060883287 | La casa de los espíritus |
| 9789500721507 | Ficciones |
