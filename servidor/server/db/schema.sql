CREATE TABLE IF NOT EXISTS libros (
    isbn TEXT PRIMARY KEY,
    titulo TEXT NOT NULL,
    total_ejemplares INTEGER NOT NULL,
    ejemplares_disponibles INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS prestamos (
    id_prestamo INTEGER PRIMARY KEY AUTOINCREMENT,
    isbn TEXT NOT NULL,
    prestatario TEXT NOT NULL,
    fecha_prestamo TEXT NOT NULL,
    fecha_devolucion TEXT NOT NULL,
    fecha_entrega TEXT,
    devuelto INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (isbn) REFERENCES libros(isbn)
);

INSERT OR IGNORE INTO libros VALUES
('9780307474278','Cien años de soledad',5,5),
('9788437604947','El amor en los tiempos del cólera',3,3),
('9788466333978','La sombra del viento',4,4);
