package com.example.alertlince

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.alertlince.databinding.FragmentThirdBinding
import com.example.alertlince.controller.UsuarioDao
import android.view.Gravity
import android.text.TextUtils


class ThirdFragment : Fragment() {

    private var _binding: FragmentThirdBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentThirdBinding.inflate(inflater, container, false)
        val view = binding.root

        // Botón flotante para agregar contacto
        binding.fabAddContact.setOnClickListener {
            mostrarDialogoAgregarContacto()
        }

        // Cargar contactos al iniciar la vista
        cargarContactos()

        return view
    }

    private fun mostrarDialogoAgregarContacto() {
        val context = requireContext()
        val num = UsuarioDao(context)

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_agregar_contacto, null)
        val nombreInput = dialogView.findViewById<EditText>(R.id.etNombre)
        val apellidoInput = dialogView.findViewById<EditText>(R.id.etApellido)
        val relacionInput = dialogView.findViewById<EditText>(R.id.etRelacion)
        val telefonoInput = dialogView.findViewById<EditText>(R.id.etTelefono)
        val correoInput = dialogView.findViewById<EditText>(R.id.etCorreo)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Agregar Contacto")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = nombreInput.text.toString()
                val apellido = apellidoInput.text.toString()
                val relacion = relacionInput.text.toString()
                val telefono = telefonoInput.text.toString()
                val correo = correoInput.text.toString()

                if (nombre.isNotEmpty() && telefono.isNotEmpty()) {
                    num.insertarContactos(nombre, apellido, relacion, telefono, correo)
                    cargarContactos()
                    Toast.makeText(context, "Contacto agregado correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Nombre y teléfono son obligatorios", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    fun cargarContactos() {
        val context = requireContext()
        val num = UsuarioDao(context)
        val contactos = num.obtenerContactos()
        actualizarTabla(contactos)
    }

    private fun actualizarTabla(contactos: List<Map<String, String>>) {
        val tableLayout = binding.root.findViewById<TableLayout>(R.id.tableContactos)
        tableLayout.removeAllViews()

        // Encabezados
        val headerRow = TableRow(context).apply {
            addView(createTextView("ID"))
            addView(createTextView("Nombre"))
            addView(createTextView("Apellido"))
            addView(createTextView("Relación"))
            addView(createTextView("Teléfono"))
            addView(createTextView("Correo"))
        }
        tableLayout.addView(headerRow)

        // Filas de datos
        contactos.forEach { contacto ->
            val row = TableRow(context).apply {
                addView(createTextView(contacto["idContacto"] ?: ""))
                addView(createTextView(contacto["nombre"] ?: ""))
                addView(createTextView(contacto["apellido"] ?: ""))
                addView(createTextView(contacto["relacion"] ?: ""))
                addView(createTextView(contacto["telefono"] ?: ""))
                addView(createTextView(contacto["correo"] ?: ""))

                setOnClickListener {
                    val idContacto = contacto["idContacto"]?.toIntOrNull()
                    if (idContacto != null) {
                        mostrarOpcionesContacto(idContacto)
                    }
                }
            }
            tableLayout.addView(row)
        }
    }

    private fun mostrarOpcionesContacto(idContacto: Int) {
        val opciones = arrayOf("Editar", "Eliminar")
        AlertDialog.Builder(requireContext())
            .setTitle("Opciones del contacto")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> editarContacto(idContacto)
                    1 -> eliminarContacto(idContacto)
                }
            }
            .show()
    }

    private fun editarContacto(idContacto: Int) {
        val context = requireContext()
        val num = UsuarioDao(context)
        val contacto = num.obtenerContactos().find { it["idContacto"]?.toIntOrNull() == idContacto } ?: return

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_editar_contacto, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreEditar)
        val etApellido = dialogView.findViewById<EditText>(R.id.etApellidoEditar)
        val etRelacion = dialogView.findViewById<EditText>(R.id.etRelacionEditar)
        val etTelefono = dialogView.findViewById<EditText>(R.id.etTelefonoEditar)
        val etCorreo = dialogView.findViewById<EditText>(R.id.etCorreoEditar)

        etNombre.setText(contacto["nombre"])
        etApellido.setText(contacto["apellido"])
        etRelacion.setText(contacto["relacion"])
        etTelefono.setText(contacto["telefono"])
        etCorreo.setText(contacto["correo"])

        AlertDialog.Builder(context)
            .setTitle("Editar Contacto")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoNombre = etNombre.text.toString().trim()
                val nuevoApellido = etApellido.text.toString().trim()
                val nuevaRelacion = etRelacion.text.toString().trim()
                val nuevoTelefono = etTelefono.text.toString().trim()
                val nuevoCorreo = etCorreo.text.toString().trim()

                num.editarContacto(
                    idContacto.toString(),
                    nuevoNombre,
                    nuevoApellido,
                    nuevaRelacion,
                    nuevoTelefono,
                    nuevoCorreo
                )

                cargarContactos()
                Toast.makeText(context, "Contacto actualizado correctamente", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarContacto(idContacto: Int) {
        val context = requireContext()
        val num = UsuarioDao(context)

        AlertDialog.Builder(context)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar este contacto?")
            .setPositiveButton("Eliminar") { _, _ ->
                num.eliminarContacto(idContacto.toString())
                cargarContactos()
                Toast.makeText(context, "Contacto eliminado correctamente", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun createTextView(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            this.gravity = Gravity.CENTER
            this.setPadding(10, 10, 10, 10)
            this.maxLines = 1
            this.ellipsize = TextUtils.TruncateAt.END
            this.layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
