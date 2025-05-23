package com.example.alertlince

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class FourthFragment : Fragment() {

    private lateinit var bluetoothStatusText: TextView
    private val esp32Name = "ESP32_ALERTA"
    private val checkHandler = Handler()
    private val checkInterval = 10000L // Verificar cada 10 segundos

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fourth, container, false)

        bluetoothStatusText = view.findViewById(R.id.bluetoothStatusText)

        // Checkboxes para configuración de alerta
        val checkboxSms = view.findViewById<CheckBox>(R.id.checkbox_sms)
        val checkboxWhatsapp = view.findViewById<CheckBox>(R.id.checkbox_whatsapp)

        val prefs = requireContext().getSharedPreferences("config_alerta", Context.MODE_PRIVATE)
        checkboxSms.isChecked = prefs.getBoolean("alerta_sms", false)
        checkboxWhatsapp.isChecked = prefs.getBoolean("alerta_whatsapp", false)

        checkboxSms.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("alerta_sms", isChecked).apply()
        }
        checkboxWhatsapp.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("alerta_whatsapp", isChecked).apply()
        }

        // Modo Claro y Oscuro
        val checkboxClaroMode = view.findViewById<CheckBox>(R.id.checkbox_claro_mode)
        val checkboxDarkMode = view.findViewById<CheckBox>(R.id.checkbox_dark_mode)

        checkboxClaroMode.isChecked = true // El modo claro está activo por defecto

        // Cambiar a modo oscuro
        checkboxDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Desmarcar el modo claro
                checkboxClaroMode.isChecked = false
                cambiarModoOscuro(view)
            } else {
                cambiarModoClaro(view)
            }
        }

        // Cambiar a modo claro
        checkboxClaroMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Desmarcar el modo oscuro
                checkboxDarkMode.isChecked = false
                cambiarModoClaro(view)
            }
        }

        val botonSesion = view.findViewById<ImageButton>(R.id.btn_sesion_menu)
        botonSesion.setOnClickListener { mostrarMenuCerrarSesion(it) }

        startBluetoothCheckLoop()
        return view
    }

    private fun cambiarModoOscuro(view: View) {
        // Cambiar el fondo a color oscuro
        view.setBackgroundColor(android.graphics.Color.rgb(58, 68, 90))  // RGB(67, 124, 177)

        // Cambiar el color de la letra a blanco en todos los TextViews
        bluetoothStatusText.setTextColor(android.graphics.Color.WHITE)  // Texto blanco

        // Cambiar el color de los Checkboxes a blanco
        val checkboxSms = view.findViewById<CheckBox>(R.id.checkbox_sms)
        val checkboxWhatsapp = view.findViewById<CheckBox>(R.id.checkbox_whatsapp)

        checkboxSms.setTextColor(android.graphics.Color.WHITE)
        checkboxWhatsapp.setTextColor(android.graphics.Color.WHITE)

        // Cambiar el color de cualquier otro texto a blanco (por ejemplo, títulos o etiquetas)
        val tituloConfig = view.findViewById<TextView>(R.id.titulo_configuracion) // Asegúrate de tener este id en tu XML
        val tituloMetodo = view.findViewById<TextView>(R.id.titulo_metodo) // Asegúrate de tener este id en tu XML
        val tituloModo = view.findViewById<TextView>(R.id.titulo_modo) // Asegúrate de tener este id en tu XML

        tituloConfig.setTextColor(android.graphics.Color.WHITE)
        tituloMetodo.setTextColor(android.graphics.Color.WHITE)
        tituloModo.setTextColor(android.graphics.Color.WHITE)
    }

    private fun cambiarModoClaro(view: View) {
        // Cambiar el fondo a color claro
        view.setBackgroundColor(android.graphics.Color.WHITE)

        // Cambiar el color de la letra a negro en todos los TextViews
        bluetoothStatusText.setTextColor(android.graphics.Color.BLACK)  // Texto negro

        // Cambiar el color de los Checkboxes a negro
        val checkboxSms = view.findViewById<CheckBox>(R.id.checkbox_sms)
        val checkboxWhatsapp = view.findViewById<CheckBox>(R.id.checkbox_whatsapp)

        checkboxSms.setTextColor(android.graphics.Color.BLACK)
        checkboxWhatsapp.setTextColor(android.graphics.Color.BLACK)

        // Cambiar el color de cualquier otro texto a negro (por ejemplo, títulos o etiquetas)
        val tituloConfig = view.findViewById<TextView>(R.id.titulo_configuracion) // Asegúrate de tener este id en tu XML
        val tituloMetodo = view.findViewById<TextView>(R.id.titulo_metodo) // Asegúrate de tener este id en tu XML
        val tituloModo = view.findViewById<TextView>(R.id.titulo_modo) // Asegúrate de tener este id en tu XML

        tituloConfig.setTextColor(android.graphics.Color.BLACK)
        tituloMetodo.setTextColor(android.graphics.Color.BLACK)
        tituloModo.setTextColor(android.graphics.Color.BLACK)
    }

    private fun mostrarMenuCerrarSesion(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add("Cerrar sesión").setOnMenuItemClickListener {
            val prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            true
        }
        popup.show()
    }

    private fun startBluetoothCheckLoop() {
        checkHandler.post(object : Runnable {
            override fun run() {
                checkBluetoothStatus()
                checkHandler.postDelayed(this, checkInterval)
            }
        })
    }

    private fun checkBluetoothStatus() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            bluetoothStatusText.text = "❌ Bluetooth no disponible o desactivado"
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothStatusText.text = "🔒 Sin permisos de Bluetooth"
            return
        }

        val isDevicePaired = bluetoothAdapter.bondedDevices.any { it.name == esp32Name }
        bluetoothStatusText.text = if (isDevicePaired) {
            "✅ Botón emparejado"
        } else {
            "❌ Botón no emparejado"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        checkHandler.removeCallbacksAndMessages(null)
    }
}
