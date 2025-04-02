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

    fun getCoordinatesFromAddress(address: String, callback: (Coordinates?) -> Unit) {
        executor.execute {
            try {
                val encodedAddress = address.replace(" ", "+")
                val url = URL("https://nominatim.openstreetmap.org/search?format=json&q=$encodedAddress")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "YourAppName/1.0 (ponponjapson@gmail.com)")

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)

                if (jsonArray.length() > 0) {
                    val firstResult = jsonArray.getJSONObject(0)
                    val lat = firstResult.getDouble("lat")
                    val lon = firstResult.getDouble("lon")

                    Handler(Looper.getMainLooper()).post {
                        callback(Coordinates(lat, lon))
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                Log.e("OSM Geocoder", "Error fetching coordinates: ${e.message}")
                Handler(Looper.getMainLooper()).post {
                    callback(null)
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
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("OSM Response", response) // Log full response

                val jsonObject = JSONObject(response)
                val addressObj = jsonObject.optJSONObject("address")

                if (addressObj != null) {
                    val street = addressObj.optString("road", "")

                    // 🔹 Now checking "quarter" for barangay, along with other possible keys
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

                    // 🏷️ Format address and remove empty fields
                    val fullAddress = listOf(street, barangay, town, city, province, postalCode, "Philippines")
                        .filter { it.isNotEmpty() }
                        .joinToString(", ")

                    Log.d("Formatted Address", fullAddress)

                    callback(fullAddress)
                } else {
                    Log.e("OSM Geocoder", "Address not found")
                    callback("Unknown Address")
                }
            } catch (e: Exception) {
                Log.e("OSM Geocoder", "Error fetching address: ${e.message}")
                callback(null)
            }
        }
    }

}

data class Coordinates(val latitude: Double, val longitude: Double)
