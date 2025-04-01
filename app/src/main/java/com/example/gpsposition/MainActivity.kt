package com.example.gpsposition

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private var compassSensor: Sensor? = null
    
    private lateinit var tvCurrentPosition: TextView
    private lateinit var tvInitialPosition: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvCompassAngle: TextView
    private lateinit var btnMarkPosition: Button
    private lateinit var btnCalculate: Button
    
    private var initialLocation: Location? = null
    private var currentAzimuth = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        tvCurrentPosition = findViewById(R.id.tvCurrentPosition)
        tvInitialPosition = findViewById(R.id.tvInitialPosition)
        tvDistance = findViewById(R.id.tvDistance)
        tvCompassAngle = findViewById(R.id.tvCompassAngle)
        btnMarkPosition = findViewById(R.id.btnMarkPosition)
        btnCalculate = findViewById(R.id.btnCalculate)

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Initialize compass sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        // Set button click listeners
        btnMarkPosition.setOnClickListener { markInitialPosition() }
        btnCalculate.setOnClickListener { calculateDistance() }

        // Request location permissions if needed
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationUpdates()
        }
    }

    override fun onResume() {
        super.onResume()
        compassSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        updateCurrentPosition(it)
                    }
                }
        }
    }

    private fun updateCurrentPosition(location: Location) {
        val positionText = "Lat: ${location.latitude}\nLon: ${location.longitude}\nAlt: ${location.altitude.roundToInt()}m"
        tvCurrentPosition.text = getString(R.string.current_position) + "\n" + positionText
    }

    private fun markInitialPosition() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        initialLocation = Location(it)
                        val positionText = "Lat: ${it.latitude}\nLon: ${it.longitude}\nAlt: ${it.altitude.roundToInt()}m"
                        tvInitialPosition.text = getString(R.string.initial_position) + "\n" + positionText
                        Toast.makeText(this, R.string.initial_position_marked, Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun calculateDistance() {
        if (initialLocation == null) {
            Toast.makeText(this, "Marque a posição inicial primeiro", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { currentLocation: Location? ->
                    currentLocation?.let {
                        val distance = initialLocation!!.distanceTo(it)
                        tvDistance.text = getString(R.string.distance_calculated, distance.roundToInt())
                    }
                }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ORIENTATION) {
                currentAzimuth = it.values[0]
                tvCompassAngle.text = getString(R.string.compass_angle) + "\n${currentAzimuth.roundToInt()}°"
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}