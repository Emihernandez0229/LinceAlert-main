package com.example.alertlince

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.io.IOException
import java.util.*

class FourthFragment : Fragment() {

    private lateinit var bluetoothStatusText: TextView
    private var bluetoothSocket: BluetoothSocket? = null
    private val esp32Name = "ESP32_ALERTA"
    private val checkHandler = Handler()
    private val checkInterval = 10000L // Verificar cada 10 segundos

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fourth, container, false)
        bluetoothStatusText = view.findViewById(R.id.bluetoothStatusText)
        startLoop()
        return view
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
        }, 1000) // Primera verificación tras 1 segundo
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
                BluetoothManager.socket = socket // 🔧 Integración clave

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
                BluetoothManager.socket = null // Limpia referencia global en caso de error
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
