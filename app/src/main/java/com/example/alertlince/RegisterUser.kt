package com.example.alertlince

import android.content.Context
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

class RegisterUser : Fragment() {

    private lateinit var email: TextInputEditText
    private lateinit var telefono: TextInputEditText
    private lateinit var password: TextInputEditText
    private lateinit var btnSignUp: Button
    private lateinit var btnInicio: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_registeruser, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        email = view.findViewById(R.id.email)
        telefono = view.findViewById(R.id.telefono)
        password = view.findViewById(R.id.password)
        btnSignUp = view.findViewById(R.id.sign_up)
        btnInicio = view.findViewById(R.id.btn_inicio)

        // Animación fade in para campos al entrar al fragment
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        email.startAnimation(fadeIn)
        telefono.startAnimation(fadeIn)
        password.startAnimation(fadeIn)
        btnSignUp.startAnimation(fadeIn)
        btnInicio.startAnimation(fadeIn)

        btnSignUp.setOnClickListener {
            // Animación scale para el botón al hacer click
            val scaleAnim = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
            btnSignUp.startAnimation(scaleAnim)

            val emailText = email.text.toString().trim()
            val telefonoText = telefono.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (!validateInput(emailText, telefonoText, passwordText)) return@setOnClickListener

            btnSignUp.isEnabled = false
            registerUser(emailText, telefonoText, passwordText)
        }

        btnInicio.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.slide_in_left, // entrada
                    android.R.anim.slide_out_right // salida
                )
                .replace(R.id.fragment_container, LoginUser())
                .commit()
        }
    }

    private fun validateInput(email: String, telefono: String, password: String): Boolean {
        if (email.isEmpty() || telefono.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(context, "Ingrese un email válido", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 8) {
            Toast.makeText(context, "La contraseña debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!telefono.matches(Regex("^\\+?[0-9]{10,15}$"))) {
            Toast.makeText(context, "Ingrese un número de teléfono válido", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun registerUser(email: String, telefono: String, contrasena: String) {
        val context = requireContext()
        val usuarioDao = UsuarioDao(context)
        val resultado = usuarioDao.insertarUsuario(email, telefono, contrasena)

        btnSignUp.isEnabled = true
        if (resultado != -1L) {  // Si la inserción fue exitosa
            val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            prefs.edit().putLong("idUsuario", resultado)
                .putBoolean("isLoggedIn", true).apply()

            Toast.makeText(context, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show()

            activity?.let {
                val intent = Intent(it, Vistas::class.java)
                it.startActivity(intent)
                requireActivity().finish()
            }
        } else {
            Toast.makeText(context, "Error al registrar usuario en la base de datos local", Toast.LENGTH_SHORT).show()
        }
    }
}