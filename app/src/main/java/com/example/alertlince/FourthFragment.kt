package com.example.alertlince

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class FourthFragment : Fragment() {

    private lateinit var bluetoothStatusText: TextView
    private val esp32Name = "ESP32_ALERTA"
    private val checkHandler = Handler()
    private val checkInterval = 10000L // Verificar cada 10 segundos
    private lateinit var checkBiometria:CheckBox


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fourth, container, false)

        checkBiometria = view.findViewById(R.id.check_biometria)

        bluetoothStatusText = view.findViewById(R.id.bluetoothStatusText)

        // Checkboxes para configuración de alerta
        val checkboxSms = view.findViewById<CheckBox>(R.id.checkbox_sms_1)
        val checkboxWhatsapp = view.findViewById<CheckBox>(R.id.checkbox_whatsapp_1)

        val prefs = requireContext().getSharedPreferences("config_alerta", Context.MODE_PRIVATE)
        checkboxSms.isChecked = prefs.getBoolean("alerta_sms", false)
        checkboxWhatsapp.isChecked = prefs.getBoolean("alerta_whatsapp", false)

        checkboxSms.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("alerta_sms", isChecked).apply()
        }
        checkboxWhatsapp.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("alerta_whatsapp", isChecked).apply()
        }

        /*// Guardar preferencias biometría
        val prefsLoginBiometria = requireContext().getSharedPreferences("preferencia_usuario", Context.MODE_PRIVATE)

        // Obtener el valor guardado correctamente con la misma clave que se usará para guardar
        val biometriaActivada = prefsLoginBiometria.getBoolean("biometria_activada", false)
        checkBiometria.isChecked = biometriaActivada

        checkBiometria.setOnCheckedChangeListener { _, isChecked ->
            val prefsLoginBiometria = requireContext().getSharedPreferences("preferencia_usuario", Context.MODE_PRIVATE)
            prefsLoginBiometria.edit().putBoolean("biometria_activada", isChecked).apply()

            // Si quieres depurar:
            Log.d("Biometria", "Check guardado: $isChecked")
        }*/


        val prefsLoginBiometria = requireContext().getSharedPreferences("preferencias_usuario", 0)
        val biometriaActivada = prefsLoginBiometria.getBoolean("biometria_activada", false)
        checkBiometria.isChecked = biometriaActivada

        checkBiometria.setOnCheckedChangeListener { _, isChecked ->
            prefsLoginBiometria.edit().putBoolean("biometria_activada", isChecked).apply()
        }






        val logoutButton = view.findViewById<LinearLayout>(R.id.logout_button)
        logoutButton.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        val rastreoBtn = view.findViewById<LinearLayout>(R.id.rastrear_ubicacion_button)
        rastreoBtn.setOnClickListener {
            // Abre tu mapa o consulta al ESP32
            val intent = Intent(requireContext(), SecondFragment::class.java)
            startActivity(intent)
        }



        startBluetoothCheckLoop()
        return view
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
