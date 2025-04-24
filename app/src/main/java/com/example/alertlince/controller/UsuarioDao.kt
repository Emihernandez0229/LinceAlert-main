package com.example.alertlince.controller

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.alertlince.model.DatabaseHelper

class UsuarioDao(private val context: Context) {
    private val dbHelper = DatabaseHelper(context)
    private val requestSmsPermission = 1

    // --- Permisos SMS ---
    fun solicitarPermisosSMS(activity: Activity) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.SEND_SMS), requestSmsPermission)
        } else {
            Log.d("Permisos", "Permiso SEND_SMS ya concedido.")
        }
    }

    fun manejarRespuestaPermiso(requestCode: Int, grantResults: IntArray) {
        if (requestCode == requestSmsPermission) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permisos", "Permiso SEND_SMS concedido.")
            } else {
                Log.e("Permisos", "Permiso SEND_SMS denegado.")
            }
        }
    }

    // --- Usuarios ---
    fun insertarUsuario(correo: String, telefono: String, contrasena: String): Long {
        val db = dbHelper.writableDatabase
        return try {
            val valores = ContentValues().apply {
                put("correo", correo)
                put("telefono", telefono)
                put("contrasena", contrasena)
            }
            db.insert("usuarios", null, valores)
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        } finally {
            db.close()
        }
    }

    fun obtenerUsuario(correo: String, contrasena: String): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM usuarios WHERE correo = ? AND contrasena = ?",
            arrayOf(correo, contrasena)
        )
        val usuarioExiste = cursor.moveToFirst()
        cursor.close()
        db.close()
        return usuarioExiste
    }


    fun guardarIdUsuarioEnSesion(idUsuario: Long) {
        val sharedPrefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putLong("idUsuario", idUsuario)
        if (editor.commit()) {
            Log.d("Sesion", "idUsuario guardado: $idUsuario")
        } else {
            Log.e("Sesion", "Error al guardar el idUsuario en las preferencias")
        }
    }

    private fun obtenerIdUsuarioDeSesion(): Long {
        val sharedPrefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        return sharedPrefs.getLong("idUsuario", -1)
    }

    // --- Contactos ---
    fun insertarContactos(nombre: String, apellido: String, relacion: String, telefono: String, correo: String): Long {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("nombre", nombre)
            put("apellido", apellido)
            put("relacionUsuario", relacion)
            put("telefono", telefono)
            put("correo", correo)
        }

        val resultado = db.insert("contactoUsuario", null, valores)
        db.close()
        return resultado
    }

    fun obtenerContactos(): List<Map<String, String>> {
        val db = dbHelper.readableDatabase
        val listaContactos = mutableListOf<Map<String, String>>()
        val cursor = db.rawQuery(
            "SELECT idContacto, nombre, apellido, relacionUsuario, telefono, correo FROM contactoUsuario",
            null
        )

        try {
            if (cursor.moveToFirst()) {
                do {
                    val contacto = mapOf(
                        "idContacto" to cursor.getInt(cursor.getColumnIndexOrThrow("idContacto")).toString(),
                        "nombre" to cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                        "apellido" to cursor.getString(cursor.getColumnIndexOrThrow("apellido")),
                        "relacion" to cursor.getString(cursor.getColumnIndexOrThrow("relacionUsuario")),
                        "telefono" to cursor.getString(cursor.getColumnIndexOrThrow("telefono")),
                        "correo" to cursor.getString(cursor.getColumnIndexOrThrow("correo"))
                    )
                    listaContactos.add(contacto)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e("DBError", "Error al leer contactos", e)
        } finally {
            cursor.close()
            db.close()
        }

        return listaContactos
    }

    fun editarContacto(id: String, nombre: String, apellido: String, relacionUsuario: String, telefono: String, correo: String): Int {
        val db = dbHelper.writableDatabase
        var resultado = 0
        try {
            if (nombre.isEmpty() || telefono.isEmpty()) {
                throw IllegalArgumentException("Nombre y teléfono son obligatorios.")
            }

            val valores = ContentValues().apply {
                put("nombre", nombre)
                put("apellido", apellido)
                put("relacionUsuario", relacionUsuario)
                put("telefono", telefono)
                put("correo", correo)
            }

            resultado = db.update("contactoUsuario", valores, "idContacto = ?", arrayOf(id))
        } catch (e: Exception) {
            Log.e("ActualizarContacto", "Error al actualizar contacto", e)
        } finally {
            db.close()
        }

        return resultado
    }

    fun eliminarContacto(id: String): Int {
        val db = dbHelper.writableDatabase
        val resultado = db.delete("contactoUsuario", "idContacto = ?", arrayOf(id))
        db.close()
        return resultado
    }

    fun obtenerNumeros(): List<String> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT telefono FROM contactoUsuario", null)
        val numeros = mutableListOf<String>()

        try {
            if (cursor.moveToFirst()) {
                val telefonoIndex = cursor.getColumnIndex("telefono")
                if (telefonoIndex >= 0) {
                    do {
                        val telefono = cursor.getString(telefonoIndex)
                        numeros.add(telefono)
                    } while (cursor.moveToNext())
                } else {
                    Log.e("DBError", "La columna 'telefono' no fue encontrada.")
                }
            }
        } catch (e: Exception) {
            Log.e("DBError", "Error al leer teléfonos", e)
        } finally {
            cursor.close()
            db.close()
        }

        return numeros
    }
}

// Clase modelo para Usuario
data class Usuario(val id: String, val email: String, val telefono: String, val password: String)
