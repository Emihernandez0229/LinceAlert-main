package com.example.alertlince

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*

class SecondFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    private var ubicacionUsuario: LatLng? = null
    private var usuarioMarker: Marker? = null
    private var smsMarker: Marker? = null
    private var rutaPolyline: Polyline? = null

    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val textoSMS = intent?.getStringExtra("texto_sms")
            textoSMS?.let {
                val coords = extraerCoordenadas(it)
                if (coords != null) {
                    smsMarker?.remove()

                    smsMarker = googleMap.addMarker(
                        MarkerOptions().position(coords).title("Ubicación recibida")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )

                    Log.d("SecondFragment", "Ubicación SMS: ${coords.latitude}, ${coords.longitude}")
                    Toast.makeText(requireContext(), "Ubicación SMS mostrada", Toast.LENGTH_SHORT).show()

                    mostrarAmbasUbicaciones(coords)
                    trazarRuta(ubicacionUsuario, coords)
                } else {
                    Toast.makeText(requireContext(), "No se detectaron coordenadas", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_second, container, false)
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            obtenerUbicacion()
        } else {
            solicitarPermisosUbicacion()
        }

        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isScrollGesturesEnabled = true
            isZoomGesturesEnabled = true
            isRotateGesturesEnabled = true
            isTiltGesturesEnabled = true
        }
    }

    private fun obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val nuevaUbicacion = LatLng(it.latitude, it.longitude)
                    ubicacionUsuario = nuevaUbicacion

                    if (usuarioMarker == null) {
                        usuarioMarker = googleMap.addMarker(
                            MarkerOptions().position(nuevaUbicacion).title("¡Tu ubicación actual!")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    } else {
                        usuarioMarker?.position = nuevaUbicacion
                    }

                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nuevaUbicacion, 15f))
                } ?: Toast.makeText(requireContext(), "No se pudo obtener ubicación", Toast.LENGTH_SHORT).show()
            }
        } else {
            solicitarPermisosUbicacion()
        }
    }

    private fun mostrarAmbasUbicaciones(ubicacionSms: LatLng) {
        val usuario = ubicacionUsuario
        if (usuario != null) {
            val builder = LatLngBounds.Builder()
            builder.include(usuario)
            builder.include(ubicacionSms)
            val bounds = builder.build()

            val padding = 100
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        } else {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ubicacionSms, 15f))
        }
    }

    private fun trazarRuta(desde: LatLng?, hasta: LatLng?) {
        if (desde != null && hasta != null) {
            rutaPolyline?.remove()
            rutaPolyline = googleMap.addPolyline(
                PolylineOptions()
                    .add(desde, hasta)
                    .color(android.graphics.Color.RED)
                    .width(6f)
            )
        }
    }

    private fun solicitarPermisosUbicacion() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun extraerCoordenadas(texto: String): LatLng? {
        val regexLink = """maps\?q=([-+]?\d+\.\d+),\s*([-+]?\d+\.\d+)""".toRegex()
        val matchLink = regexLink.find(texto)
        if (matchLink != null) {
            val lat = matchLink.groupValues[1].toDoubleOrNull()
            val lng = matchLink.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null) return LatLng(lat, lng)
        }

        val regexCoords = """([-+]?\d+\.\d+),\s*([-+]?\d+\.\d+)""".toRegex()
        val matchCoords = regexCoords.find(texto)
        if (matchCoords != null) {
            val lat = matchCoords.groupValues[1].toDoubleOrNull()
            val lng = matchCoords.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null) return LatLng(lat, lng)
        }

        return null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                googleMap.isMyLocationEnabled = true
                obtenerUbicacion()
            }
        } else {
            Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(smsReceiver, IntentFilter("sms_ubicacion_recibida"))
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(smsReceiver)
        mapView.onStop()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
