package com.example.alertlince

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.io.IOException
import java.util.*

class FourthFragment : Fragment() {

    private lateinit var bluetoothStatusText: TextView
    private var bluetoothSocket: BluetoothSocket? = null
    private val esp32Name = "ESP32_ALERTAaa"
    private val checkHandler = Handler()
    private val checkInterval = 10000L // Verificar cada 10 segundos

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fourth, container, false)

        bluetoothStatusText = view.findViewById(R.id.bluetoothStatusText)

        // Referencias a los CheckBox
        val checkboxSms = view.findViewById<CheckBox>(R.id.checkbox_sms)
        val checkboxWhatsapp = view.findViewById<CheckBox>(R.id.checkbox_whatsapp)

        // Leer y aplicar estado guardado
        val prefs = requireContext().getSharedPreferences("config_alerta", Context.MODE_PRIVATE)
        checkboxSms.isChecked = prefs.getBoolean("alerta_sms", false)
        checkboxWhatsapp.isChecked = prefs.getBoolean("alerta_whatsapp", false)

        // Guardar cuando cambian
        checkboxSms.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("alerta_sms", isChecked).apply()
        }

        checkboxWhatsapp.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("alerta_whatsapp", isChecked).apply()
        }

        // Botón de menú de sesión
        val botonSesion = view.findViewById<ImageButton>(R.id.btn_sesion_menu)
        botonSesion.setOnClickListener {
            mostrarMenuCerrarSesion(it)
        }

        startLoop()
        return view
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

    private fun startLoop() {
        checkHandler.postDelayed(object : Runnable {
            override fun run() {
                if (bluetoothSocket == null || !isSocketConnected(bluetoothSocket)) {
                    connectToESP32()
                } else {
                    bluetoothStatusText.text = "✅ Conectado al botón físico"
                }
                checkHandler.postDelayed(this, checkInterval)
            }
        }, 1000)
    }

    private fun connectToESP32() {
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

        val espDevice = bluetoothAdapter.bondedDevices.firstOrNull { it.name == esp32Name }

        if (espDevice == null) {
            bluetoothStatusText.text = "❌ Botón no emparejado"
            return
        }

        Thread {
            try {
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                val socket = espDevice.createRfcommSocketToServiceRecord(uuid)
                socket.connect()

                bluetoothSocket = socket
                BluetoothManager.socket = socket

                activity?.runOnUiThread {
                    bluetoothStatusText.text = "✅ Conectado al botón físico"
                    Toast.makeText(requireContext(), "Conexión establecida con ESP32", Toast.LENGTH_SHORT).show()
                }

            } catch (e: IOException) {
                activity?.runOnUiThread {
                    bluetoothStatusText.text = "❌ Falló la conexión"
                }
                try {
                    bluetoothSocket?.close()
                } catch (_: IOException) { }
                bluetoothSocket = null
                BluetoothManager.socket = null
            }
        }.start()
    }

    private fun isSocketConnected(socket: BluetoothSocket?): Boolean {
        return try {
            socket?.outputStream?.write("".toByteArray())
            true
        } catch (e: IOException) {
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        checkHandler.removeCallbacksAndMessages(null)
        try {
            bluetoothSocket?.close()
        } catch (_: IOException) { }
        bluetoothSocket = null
        BluetoothManager.socket = null
    }
}
