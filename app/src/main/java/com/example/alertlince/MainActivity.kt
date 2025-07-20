package com.example.alertlince

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.alertlince.model.DatabaseHelper

class MainActivity : AppCompatActivity() {

    private val REQUEST_SMS_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa base de datos
        val dbHelper = DatabaseHelper(this)
        dbHelper.writableDatabase

        solicitarPermisos()

        // Verificamos si la sesión está activa
        val prefsSesion = getSharedPreferences("UserSession", MODE_PRIVATE)
        val isLoggedIn = prefsSesion.getBoolean("isLoggedIn", false)

        // Verificamos si la biometría está activada
        val prefsBiometria = getSharedPreferences("preferencias_usuario", MODE_PRIVATE)
        val biometriaActivada = prefsBiometria.getBoolean("biometria_activada", false)

        Log.d("Biometria", "¿Sesión activa?: $isLoggedIn, ¿Biometría activada?: $biometriaActivada")

        if (isLoggedIn) {
            if (biometriaActivada) {
                autenticarConBiometria()
            } else {
                // ir a la vista de login
                loadFragment(LoginUser())
            }
        } else {
            // Mostrar fragmento de login
            if (savedInstanceState == null) {
                loadFragment(LoginUser())
            }
        }
    }

    private fun autenticarConBiometria() {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(this)
            val biometricPrompt = BiometricPrompt(this, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        startActivity(Intent(this@MainActivity, Vistas::class.java))
                        finish()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        Toast.makeText(this@MainActivity, "Error biometría: $errString", Toast.LENGTH_SHORT).show()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        Toast.makeText(this@MainActivity, "Autenticación fallida", Toast.LENGTH_SHORT).show()
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticación biométrica requerida")
                .setSubtitle("Confirma tu identidad para continuar")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            // Si no se puede usar biometría, pasa directo
            startActivity(Intent(this, Vistas::class.java))
            finish()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun solicitarPermisos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), REQUEST_SMS_PERMISSION)
        } else {
            Log.d("Permisos", "El permiso de SMS ya fue concedido.")
        }
    }

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
