package com.example.alertlince

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.alertlince.controller.UsuarioDao
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import java.util.*

class FirstFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.homeprincipal, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val btnAlerta: Button = view.findViewById(R.id.btn_alerta)
        btnAlerta.setOnClickListener {
            ejecutarAlerta()
        }

        inicializarBluetoothListener()
        return view
    }

    private fun ejecutarAlerta() {
        val prefs = requireContext().getSharedPreferences("config_alerta", Context.MODE_PRIVATE)
        val enviarSMS = prefs.getBoolean("alerta_sms", false)
        val enviarWhatsapp = prefs.getBoolean("alerta_whatsapp", false)

        if (!enviarSMS && !enviarWhatsapp) {
            Toast.makeText(requireContext(), "Debe activar al menos un método de envío", Toast.LENGTH_SHORT).show()
            return
        }

        obtenerUbicacion { mensajeSMS, mensajeWhatsapp ->
            if (mensajeSMS != null && mensajeWhatsapp != null) {
                if (enviarSMS) sendSms(mensajeSMS)
                if (enviarWhatsapp) sendWhatsappMessage(mensajeWhatsapp)
            } else {
                Toast.makeText(requireContext(), "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun obtenerUbicacion(callback: (String?, String?) -> Unit) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            callback(null, null)
            return
        }

        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            callback(null, null)
            return
        }

        val task: Task<Location> = fusedLocationClient.lastLocation
        task.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude
                //val mensajeSMS = "¡Alerta de emergencia! Ubicación: Lat: $lat, Lng: $lng"
                val msmAsd = "!Alerta!"
                val mensajeSMS = "$msmAsd https://www.google.com/maps?q=$lat,$lng"
                val mensajeWhatsapp = "¡Alerta de emergencia! Estoy aquí: https://www.google.com/maps?q=$lat,$lng"
                callback(mensajeSMS, mensajeWhatsapp)
            } else {
                callback(null, null)
            }
        }
    }

    private fun sendSms(message: String) {
        val context = requireContext()
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.SEND_SMS), 2)
            Toast.makeText(context, "Permiso para enviar SMS no concedido", Toast.LENGTH_SHORT).show()
            return
        }

        val conController = UsuarioDao(context)
        val conNumeros = conController.obtenerNumeros()
        val smsManager = SmsManager.getDefault()

        for (numero in conNumeros) {
            try {
                smsManager.sendTextMessage(numero, null, message, null, null)
                Log.d("SMS", "Mensaje enviado a: $numero")
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error al enviar SMS a $numero", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendWhatsappMessage(message: String) {
        val context = requireContext()
        val conController = UsuarioDao(context)
        val conNumeros = conController.obtenerNumeros()

        if (conNumeros.isNotEmpty()) {
            val primerNumero = conNumeros[0].replace("\\D".toRegex(), "")
            try {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.setPackage("com.whatsapp")
                intent.putExtra(Intent.EXTRA_TEXT, message)
                intent.putExtra("jid", "52$primerNumero@s.whatsapp.net")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "WhatsApp no está instalado o hubo un error", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            Toast.makeText(context, "No hay números registrados", Toast.LENGTH_SHORT).show()
        }
    }

    private fun inicializarBluetoothListener() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                3
            )
            Toast.makeText(requireContext(), "Faltan permisos de Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        val device = bluetoothAdapter?.bondedDevices?.find { it.name == "ESP32_ALERTA" }

        if (device != null) {
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
            val socket = device.createRfcommSocketToServiceRecord(uuid)

            Thread {
                try {
                    bluetoothAdapter.cancelDiscovery()
                    socket.connect()

                    val input = socket.inputStream
                    val buffer = ByteArray(1024)

                    while (true) {
                        val bytes = input.read(buffer)
                        val received = buffer.decodeToString(0, bytes).trim()
                        Log.d("Bluetooth", "Mensaje recibido: $received")

                        if (received == "SEND_ALERT") {
                            requireActivity().runOnUiThread {
                                ejecutarAlerta()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Error al conectar con ESP32", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        } else {
            Toast.makeText(requireContext(), "ESP32_ALERTA no emparejado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ejecutarAlerta()
            }
            2 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Permiso para enviar SMS concedido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Permiso para enviar SMS denegado", Toast.LENGTH_SHORT).show()
            }
            3 -> if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                inicializarBluetoothListener()
            } else {
                Toast.makeText(requireContext(), "Permisos Bluetooth denegados", Toast.LENGTH_SHORT).show()
            }
        }
    }
}