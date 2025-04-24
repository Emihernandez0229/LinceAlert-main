package com.example.alertlince

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.alertlince.model.DatabaseHelper

class MainActivity : AppCompatActivity() {

    private val REQUEST_SMS_PERMISSION = 1  // Código para la solicitud de permisos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔥 Inicializar la base de datos automáticamente
        val dbHelper = DatabaseHelper(this)
        dbHelper.writableDatabase // Esto crea la BD si no existe

        // 📩 Solicitar permisos si no están concedidos
        solicitarPermisos()

        // 🔑 Verificar si hay sesión iniciada
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            // Si el usuario ya está autenticado, redirigirlo a la actividad principal (Vistas)
            startActivity(Intent(this, Vistas::class.java))
            finish() // Evita que el usuario vuelva al login con el botón "atrás"
        } else {
            // Si no hay sesión, mostrar el fragmento de LoginUser
            if (savedInstanceState == null) {
                loadFragment(LoginUser())
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    // 🔹 Función para solicitar permisos en tiempo de ejecución
    private fun solicitarPermisos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), REQUEST_SMS_PERMISSION)
        } else {
            Log.d("Permisos", "El permiso de SMS ya fue concedido.")
        }
    }

    // 🔹 Manejo de la respuesta del usuario al solicitar permisos
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permisos", "Permiso de SMS concedido")
            } else {
                Log.e("Permisos", "Permiso de SMS denegado")
            }
        }
    }
}
