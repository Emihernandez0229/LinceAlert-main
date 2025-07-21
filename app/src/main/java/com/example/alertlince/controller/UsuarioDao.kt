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
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.util.Base64

class UsuarioDao(private val context: Context) {
    private val dbHelper = DatabaseHelper(context)
    private val requestSmsPermission = 1

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

    // Inserta usuario y lo activa guardando la sesión
    fun registrarYActivarUsuario(correo: String, telefono: String, contrasena: String): Boolean {
        val id = insertarUsuario(correo, telefono, contrasena)
        return if (id != -1L) {
            guardarIdUsuarioEnSesion(id)
            true
        } else {
            false
        }
    }

    // Inserta un usuario nuevo (solo inserción)
    fun insertarUsuario(correo: String, telefono: String, contrasena: String): Long {
        val db = dbHelper.writableDatabase
        return try {
            val hash = hashPassword(contrasena)  // Hashea la contraseña con salt

            val valores = ContentValues().apply {
                put("correo", correo)
                put("telefono", telefono)
                put("contrasena", hash)
            }
            db.insert("usuarios", null, valores)
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        } finally {
            db.close()
        }
    }

    // Loguea usuario y guarda la sesión
    fun loginUsuario(correo: String, contrasena: String): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT idUsuario, contrasena FROM usuarios WHERE correo = ?",
            arrayOf(correo)
        )
        val resultado = if (cursor.moveToFirst()) {
            val storedHash = cursor.getString(cursor.getColumnIndexOrThrow("contrasena"))
            val id = cursor.getLong(cursor.getColumnIndexOrThrow("idUsuario"))
            if (verifyPassword(contrasena, storedHash)) {
                guardarIdUsuarioEnSesion(id)
                true
            } else false
        } else {
            false
        }
        cursor.close()
        db.close()
        return resultado
    }

    // Obtiene el id del usuario activo en sesión
    fun obtenerIdUsuarioDeSesion(): Long {
        val sharedPrefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        return sharedPrefs.getLong("idUsuario", -1)
    }

    // Guarda el id del usuario activo en SharedPreferences
    private fun guardarIdUsuarioEnSesion(idUsuario: Long) {
        val sharedPrefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        sharedPrefs.edit().putLong("idUsuario", idUsuario).apply()
    }

    // Cierra sesión borrando el usuario activo
    fun cerrarSesion() {
        val sharedPrefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        sharedPrefs.edit().remove("idUsuario").apply()
    }

    // Obtiene los contactos del usuario activo
    fun obtenerContactos(): List<Map<String, String>> {
        val idUsuario = obtenerIdUsuarioDeSesion()
        if (idUsuario == -1L) return emptyList()

        val db = dbHelper.readableDatabase
        val listaContactos = mutableListOf<Map<String, String>>()

        val cursor = db.rawQuery(
            "SELECT idContacto, nombre, apellido, relacionUsuario, telefono, correo FROM contactoUsuario WHERE idUsuario = ?",
            arrayOf(idUsuario.toString())
        )

        try {
            if (cursor.moveToFirst()) {
                do {
                    val columnIndexIdContacto = cursor.getColumnIndex("idContacto")
                    val columnIndexNombre = cursor.getColumnIndex("nombre")
                    val columnIndexApellido = cursor.getColumnIndex("apellido")
                    val columnIndexRelacion = cursor.getColumnIndex("relacionUsuario")
                    val columnIndexTelefono = cursor.getColumnIndex("telefono")
                    val columnIndexCorreo = cursor.getColumnIndex("correo")

                    if (columnIndexIdContacto >= 0 && columnIndexNombre >= 0 && columnIndexApellido >= 0 &&
                        columnIndexRelacion >= 0 && columnIndexTelefono >= 0 && columnIndexCorreo >= 0) {

                        val contacto = mapOf(
                            "idContacto" to cursor.getInt(columnIndexIdContacto).toString(),
                            "nombre" to cursor.getString(columnIndexNombre),
                            "apellido" to cursor.getString(columnIndexApellido),
                            "relacion" to cursor.getString(columnIndexRelacion),
                            "telefono" to cursor.getString(columnIndexTelefono),
                            "correo" to cursor.getString(columnIndexCorreo)
                        )
                        listaContactos.add(contacto)
                    } else {
                        Log.e("Error", "Una o más columnas no se encuentran en el cursor.")
                    }
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor.close()
            db.close()
        }

        return listaContactos
    }

    // Obtiene los números del usuario activo
    fun obtenerNumeros(): List<String> {
        val idUsuario = obtenerIdUsuarioDeSesion()
        if (idUsuario == -1L) return emptyList()

        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT telefono FROM contactoUsuario WHERE idUsuario = ?",
            arrayOf(idUsuario.toString())
        )
        val numeros = mutableListOf<String>()

        if (cursor.moveToFirst()) {
            val telefonoIndex = cursor.getColumnIndex("telefono")
            if (telefonoIndex >= 0) {
                do {
                    val telefono = cursor.getString(telefonoIndex)
                    numeros.add(telefono)
                } while (cursor.moveToNext())
            } else {
                Log.e("DBError", "La columna 'telefono' no fue encontrada en la base de datos.")
            }
        } else {
            Log.e("DBInfo", "No se encontraron resultados en la base de datos.")
        }

        cursor.close()
        db.close()
        return numeros
    }

    // Inserta un contacto ligado al usuario activo
    fun insertarContactos(nombre: String, apellido: String, relacion: String, telefono: String, correo: String): Long {
        val idUsuario = obtenerIdUsuarioDeSesion()
        if (idUsuario == -1L) return -1L

        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("nombre", nombre)
            put("apellido", apellido)
            put("relacionUsuario", relacion)
            put("telefono", telefono)
            put("correo", correo)
            put("idUsuario", idUsuario)
        }

        val resultado = db.insert("contactoUsuario", null, valores)
        db.close()
        return resultado
    }

    // Edita un contacto asegurándose que pertenece al usuario activo
    fun editarContacto(idContacto: String, nombre: String, apellido: String, relacionUsuario: String, telefono: String, correo: String): Int {
        val idUsuario = obtenerIdUsuarioDeSesion()
        if (idUsuario == -1L) return 0

        val db = dbHelper.writableDatabase
        var resultado = 0
        try {
            if (nombre.isEmpty() || apellido.isEmpty() || telefono.isEmpty() || correo.isEmpty()) {
                throw IllegalArgumentException("Todos los campos deben ser completados.")
            }

            val valores = ContentValues().apply {
                put("nombre", nombre)
                put("apellido", apellido)
                put("relacionUsuario", relacionUsuario)
                put("telefono", telefono)
                put("correo", correo)
            }

            // Solo actualiza si el contacto pertenece al usuario activo
            resultado = db.update("contactoUsuario", valores, "idContacto = ? AND idUsuario = ?", arrayOf(idContacto, idUsuario.toString()))
        } catch (e: Exception) {
            Log.e("ActualizarUsuario", "Error al actualizar contacto", e)
        } finally {
            db.close()
        }

        return resultado
    }

    // Elimina un contacto asegurándose que pertenece al usuario activo
    fun eliminarContacto(idContacto: String): Int {
        val idUsuario = obtenerIdUsuarioDeSesion()
        if (idUsuario == -1L) return 0

        val db = dbHelper.writableDatabase
        val resultado = db.delete("contactoUsuario", "idContacto = ? AND idUsuario = ?", arrayOf(idContacto, idUsuario.toString()))
        db.close()
        return resultado
    }
}

// Funciones para hash y verificación de contraseñas
fun hashPassword(password: String, salt: ByteArray = SecureRandom().generateSeed(16)): String {
    val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val hash = factory.generateSecret(spec).encoded
    val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
    val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)
    return "$saltBase64:$hashBase64"
}

fun verifyPassword(password: String, stored: String): Boolean {
    val parts = stored.split(":")
    if (parts.size != 2) return false
    val salt = Base64.decode(parts[0], Base64.NO_WRAP)
    val hashOfInput = hashPassword(password, salt)
    return constantTimeEquals(stored, hashOfInput)
}

fun constantTimeEquals(a: String, b: String): Boolean {
    if (a.length != b.length) return false
    var result = 0
    for (i in a.indices) {
        result = result or (a[i].code xor b[i].code)
    }
    return result == 0
}
