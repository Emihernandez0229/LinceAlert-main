package com.example.alertlince

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.alertlince.controller.UsuarioDao
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginUser : Fragment() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var signInButton: Button
    //private lateinit var switchBiometria: SwitchMaterial
    private lateinit var signUpTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.activity_loginuser, container, false)

        // Referencias a vistas
        emailEditText = rootView.findViewById(R.id.email)
        passwordEditText = rootView.findViewById(R.id.password)
        emailLayout = rootView.findViewById(R.id.email_layout)
        passwordLayout = rootView.findViewById(R.id.password_layout)
        signInButton = rootView.findViewById(R.id.btn_signin)
        //switchBiometria = rootView.findViewById(R.id.switch_biometria) // este se cambio a fourth fragment
        signUpTextView = rootView.findViewById(R.id.sign_up)

        // Animaciones de entrada
        val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in)
        emailEditText.startAnimation(fadeIn)
        passwordEditText.startAnimation(fadeIn)
        signInButton.startAnimation(fadeIn)
        //switchBiometria.startAnimation(fadeIn)
        signUpTextView.startAnimation(fadeIn)

        /*// Preferencias biometría
        val prefs = requireContext().getSharedPreferences("preferencias_usuario", 0)
        val biometriaActivada = prefs.getBoolean("biometria_activada", false)
        switchBiometria.isChecked = biometriaActivada

        switchBiometria.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("biometria_activada", isChecked).apply()
        }*/

        signInButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (!validateInput(email, password)) return@setOnClickListener

            login(email, password)
        }

        signUpTextView.setOnClickListener {
            val registerUserFragment = RegisterUser()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, registerUserFragment)
                .addToBackStack(null)
                .commit()
        }

        return rootView
    }

    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true
        emailLayout.error = null
        passwordLayout.error = null

        if (email.isEmpty()) {
            emailLayout.error = "Campo requerido"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Correo no válido"
            isValid = false
        }

        if (password.isEmpty()) {
            passwordLayout.error = "Campo requerido"
            isValid = false
        } else if (password.length < 6) {
            passwordLayout.error = "Mínimo 6 caracteres"
            isValid = false
        }

        return isValid
    }

    private fun login(email: String, password: String) {
        val context = requireContext()
        val usuarioDao = UsuarioDao(context)
        val resultado = usuarioDao.loginUsuario(email, password)

        if (resultado) {
            val prefs = requireContext().getSharedPreferences("UserSession", 0)
            prefs.edit().putBoolean("isLoggedIn", true).apply()


            Toast.makeText(context, "Sesión exitosa", Toast.LENGTH_SHORT).show()
            activity?.let {
                val intent = Intent(it, Vistas::class.java)
                it.startActivity(intent)
                requireActivity().finish()
            }
        } else {
            // Shake animation al fallar
            val shake = AnimationUtils.loadAnimation(context, R.anim.shake)
            emailEditText.startAnimation(shake)
            passwordEditText.startAnimation(shake)

            emailLayout.error = "Credenciales incorrectas"
            passwordLayout.error = "Credenciales incorrectas"
        }
    }

    /*private fun mostrarAutenticacionBiometrica(onSuccess: () -> Unit) {
        val biometricManager = BiometricManager.from(requireContext())
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(requireContext(), "La autenticación biométrica no está disponible", Toast.LENGTH_LONG).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(requireContext())

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(requireContext(), "Error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(requireContext(), "Autenticación fallida", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación biométrica")
            .setSubtitle("Confirma tu identidad")
            .setDescription("Usa huella, rostro o PIN")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }*/
}
