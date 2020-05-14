package com.android.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.weather.databinding.FragmentWeatherBinding
import com.android.weather.network.GeoApi
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private const val LOCATION_PERMISSION_REQUEST = 1
private const val LOCATION_PERMISSION = "android.permission.ACCESS_FINE_LOCATION"

class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!
    lateinit var fusedLocationClient: FusedLocationProviderClient

    var latitude: Double = 0.0
    var longitude: Double = 0.0

    override fun onCreateView(inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentWeatherBinding.inflate(inflater, container, false)

        binding.getCoordinatesButton.setOnClickListener { getLastLocation() }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        getLastLocation()
    }

    private fun loadWeatherAndUpdate() {
        // Launch Kotlin Coroutine on Android's main thread
        GlobalScope.launch(Dispatchers.Main) {
            // Execute web request through coroutine call adapter & retrofit
            //val webResponse = GeoApi.retrofitService.getGeo().await()

            val webResponse = GeoApi.retrofitService.getWeatherByGps(latitude, longitude,
                "f20ee5d768c40c7094c1380400bf5a58").await()

           println(webResponse.raw().toString())

            if (webResponse.isSuccessful) {
                // Get the returned & parsed JSON from the web response.
                // Type specified explicitly here to make it clear that we already
                // get parsed contents.
                val partList = webResponse.body()

                val weather = partList?.weather
                val main = partList?.main
                val wind = partList?.wind

                binding.desciptionView.text = weather?.get(0)?.desc
                binding.temperatureView.text = getString(R.string.temperature, main?.temperature?.celcius())
                binding.temperatureFeelsLikeView.text = getString(R.string.temperature_feels_like, main?.temperature_feels_like?.celcius())
                binding.pressureView.text = getString(R.string.pressure, main?.pressure?.mmHg())
                binding.humidityView.text = getString(R.string.humidity, main?.humidity?.noSignAfterDot())
                binding.speedView.text = getString(R.string.speed, wind?.speed?.oneSignAfterDot())

                binding.myCityView.text = partList?.city
            } else {
                // Print error information
                Toast.makeText(context, "Error ${webResponse.code()}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(requireContext(), LOCATION_PERMISSION)
            == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
    }

    private fun requestPermissions() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            LOCATION_PERMISSION_REQUEST ->
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Granted. Start getting the location information

                }
                else {

                }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location == null) {
                        requestNewLocationData()
                    } else {
//                        binding.latitudeView.text = location.latitude.toString()
//                        binding.longitudeView.text = location.longitude.toString()
                        latitude = location.latitude
                        longitude = location.longitude
                        loadWeatherAndUpdate()
                    }
                }
            } else {
                Toast.makeText(context, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
//            binding.latitudeView.text = mLastLocation.latitude.toString()
//            binding.longitudeView.text = mLastLocation.longitude.toString()
        }
    }

}
