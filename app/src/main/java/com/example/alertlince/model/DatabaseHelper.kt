package com.example.alertlince.model

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // Crear tablas en la base de datos
        db.execSQL(CREATE_TABLE_USUARIOS)
        db.execSQL(CREATE_TABLE_CAMARAS)
        db.execSQL(CREATE_TABLE_ALERTAS)
        db.execSQL(CREATE_TABLE_CONTACTO_USUARIO)
        db.execSQL(CREATE_TABLE_UBICACION)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Eliminar tablas si ya existen y volver a crearlas
        db.execSQL("DROP TABLE IF EXISTS ubicacion")
        db.execSQL("DROP TABLE IF EXISTS contactoUsuario")
        db.execSQL("DROP TABLE IF EXISTS alerta")
        db.execSQL("DROP TABLE IF EXISTS camara")
        db.execSQL("DROP TABLE IF EXISTS usuarios")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "AlertaLince.db"
        private const val DATABASE_VERSION = 1

        // Tabla usuarios
        private const val CREATE_TABLE_USUARIOS = """
            CREATE TABLE usuarios (
                idUsuario INTEGER PRIMARY KEY AUTOINCREMENT,
                correo TEXT NOT NULL UNIQUE,
                telefono TEXT NOT NULL UNIQUE,
                contrasena TEXT NOT NULL
            )
        """

        // Tabla cámaras
        private const val CREATE_TABLE_CAMARAS = """
            CREATE TABLE camara (
                idCamara INTEGER PRIMARY KEY AUTOINCREMENT,
                estado TEXT NOT NULL,
                fechaInicioGrabacion DATETIME,
                fechaFinalGrabacion DATETIME,
                eventoDetectado BOOLEAN,
                idUsuario INTEGER NOT NULL,
                FOREIGN KEY (idUsuario) REFERENCES usuarios(idUsuario) ON DELETE CASCADE
            )
        """
        // Tabla ubicación
        private const val CREATE_TABLE_UBICACION = """
            CREATE TABLE ubicacion (
                idUbicacion INTEGER PRIMARY KEY AUTOINCREMENT,
                latitud REAL NOT NULL,
                longitud REAL NOT NULL,
                idUsuario INTEGER NOT NULL,
                idCamara INTEGER NOT NULL,
                FOREIGN KEY (idUsuario) REFERENCES usuarios(idUsuario) ON DELETE CASCADE,
                FOREIGN KEY (idCamara) REFERENCES camara(idCamara) ON DELETE CASCADE
            )
        """

        // Tabla alertas
        private const val CREATE_TABLE_ALERTAS = """
            CREATE TABLE alerta (
                idAlerta INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha DATE NOT NULL,
                hora TIME NOT NULL,
                notificacionEnviada BOOLEAN,
                estado TEXT NOT NULL,
                idUsuario INTEGER NOT NULL,
                idCamara INTEGER NOT NULL,
                idUbicacion INTEGER NOT NULL,
                FOREIGN KEY (idUsuario) REFERENCES usuarios(idUsuario) ON DELETE CASCADE,
                FOREIGN KEY (idCamara) REFERENCES camara(idCamara) ON DELETE CASCADE,
                FOREIGN KEY (idubicacion) REFERENCES ubicacion(idUbicacion) ON DELETE CASCADE
            )
        """

        // Tabla contactoUsuario
        private const val CREATE_TABLE_CONTACTO_USUARIO = """
            CREATE TABLE contactoUsuario (
                idContacto INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                apellido TEXT NOT NULL,
                relacionUsuario TEXT NOT NULL,
                telefono TEXT NOT NULL,
                correo TEXT NOT NULL
            )
        """


    }
}
