package com.example.alertlince

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.alertlince.controller.UsuarioDao
import com.google.android.material.textfield.TextInputEditText

class LoginUser : Fragment() {

    private lateinit var correoEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var signInButton: Button
    private lateinit var signUpTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.activity_loginuser, container, false)

        // Referencias a vistas
        correoEditText = rootView.findViewById(R.id.email)  // Puedes renombrar el ID a "correo" en el XML para mayor coherencia
        passwordEditText = rootView.findViewById(R.id.password)
        signInButton = rootView.findViewById(R.id.btn_signin)
        signUpTextView = rootView.findViewById(R.id.sign_up)

        // Clic en botón de inicio de sesión
        signInButton.setOnClickListener {
            val correo = correoEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (validateInput(correo, password)) {
                signInButton.isEnabled = false
                login(correo, password)
            }
        }

        // Clic en enlace de registro
        signUpTextView.setOnClickListener {
            val registerUserFragment = RegisterUser()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, registerUserFragment)
                .addToBackStack(null)
                .commit()
        }

        return rootView
    }

    private fun validateInput(correo: String, password: String): Boolean {
        if (correo.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(context, "Ingrese un correo válido", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 6) {
            Toast.makeText(context, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun login(correo: String, password: String) {
        val context = requireContext()
        val usuarioDao = UsuarioDao(context)
        val resultado = usuarioDao.obtenerUsuario(correo, password)

        signInButton.isEnabled = true
        if (resultado) {
            Toast.makeText(context, "Sesión exitosa", Toast.LENGTH_SHORT).show()
            activity?.let {
                val intent = Intent(it, Vistas::class.java)
                it.startActivity(intent)
                requireActivity().finish()
            }
        } else {
            Toast.makeText(context, "Error de Autenticación", Toast.LENGTH_SHORT).show()
        }
    }
}
