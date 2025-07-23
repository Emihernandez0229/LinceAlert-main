package com.example.alertlince

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast


class DialogAlert {
    fun mostrarVentanaEmergente(
        context: Context,
        numeroActual: String? = null,
        onNumeroIngresado: (String) -> Unit
    ) {
        val editText = EditText(context).apply {
            hint = "Introduce un número"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(numeroActual) // ← aquí precargas el número si existe
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(editText)
        }

        AlertDialog.Builder(context)
            .setTitle("Ingrese un número")
            .setView(layout)
            .setPositiveButton("Aceptar") { dialog, _ ->
                val numero = editText.text.toString()
                if (numero.isNotEmpty()) {
                    onNumeroIngresado(numero)
                } else {
                    Toast.makeText(context, "No ingresaste un número", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
            .show()
    }
}
