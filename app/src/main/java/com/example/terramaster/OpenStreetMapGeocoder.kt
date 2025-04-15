package com.example.terramaster

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class OpenStreetMapGeocoder(private val context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private val userAgent = "TerraMaster/1.0 (ponponjapson@gmail.com)" // Update with your app info

    fun getCoordinatesFromAddress(address: String, callback: (Coordinates?) -> Unit) {
        executor.execute {
            var retries = 3
            var success = false
            while (retries > 0 && !success) {
                try {
                    val encodedAddress = address.replace(" ", "+")
                    val url = URL("https://nominatim.openstreetmap.org/search?format=json&q=$encodedAddress")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", userAgent)
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)

                    if (jsonArray.length() > 0) {
                        val firstResult = jsonArray.getJSONObject(0)
                        val lat = firstResult.getString("lat").toDouble()
                        val lon = firstResult.getString("lon").toDouble()

                        Handler(Looper.getMainLooper()).post {
                            callback(Coordinates(lat, lon))
                        }
                        success = true
                    } else {
                        Handler(Looper.getMainLooper()).post { callback(null) }
                    }
                } catch (e: Exception) {
                    Log.e("OSM Geocoder", "Error fetching coordinates: ${e.message}", e)
                    retries--
                    if (retries == 0) {
                        Handler(Looper.getMainLooper()).post { callback(null) }
                    }
                    Thread.sleep(2000) // Wait for 2 seconds before retrying
                }
            }
        }
    }


    fun getAddressFromCoordinates(lat: Double, lon: Double, callback: (String?) -> Unit) {
        executor.execute {
            try {
                val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", userAgent)
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("OSM Response", response)

                val jsonObject = JSONObject(response)
                val addressObj = jsonObject.optJSONObject("address")

                if (addressObj != null) {
                    val street = addressObj.optString("road", "")
                    val barangay = addressObj.optString("quarter",
                        addressObj.optString("suburb",
                            addressObj.optString("village",
                                addressObj.optString("hamlet", "")
                            )
                        )
                    )
                    val town = addressObj.optString("town", addressObj.optString("municipality", ""))
                    val city = addressObj.optString("city", addressObj.optString("county", ""))
                    val province = addressObj.optString("region", addressObj.optString("state", ""))
                    val postalCode = addressObj.optString("postcode", "")

                    val fullAddress = listOf(street, barangay, town, city, province, postalCode, "Philippines")
                        .filter { it.isNotEmpty() }
                        .joinToString(", ")

                    Log.d("Formatted Address", fullAddress)

                    Handler(Looper.getMainLooper()).post {
                        callback(fullAddress)
                    }
                } else {
                    Log.e("OSM Geocoder", "No address found for: $lat, $lon")
                    Handler(Looper.getMainLooper()).post {
                        callback("Unknown Address")
                    }
                }
            } catch (e: Exception) {
                Log.e("OSM Geocoder", "Error fetching address: ${e.message}", e)
                Handler(Looper.getMainLooper()).post {
                    callback(null)
                }
            }
        }
    }
}

data class Coordinates(val latitude: Double, val longitude: Double)
