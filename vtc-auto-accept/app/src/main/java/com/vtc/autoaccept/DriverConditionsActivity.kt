package com.vtc.autoaccept

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DriverConditionsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conditions)
        prefs = getSharedPreferences("config", MODE_PRIVATE)

        val etMinPrice = findViewById<EditText>(R.id.etMinPrice)
        val etMaxDistancePickup = findViewById<EditText>(R.id.etMaxDistancePickup)
        val etMinPricePerKm = findViewById<EditText>(R.id.etMinPricePerKm)
        val etMaxRideDistance = findViewById<EditText>(R.id.etMaxRideDistance)
        val switchAvoidLowRated = findViewById<Switch>(R.id.switchAvoidLowRated)
        val btnSelectApp = findViewById<Button>(R.id.btnSelectApp)
        val tvSelectedApp = findViewById<TextView>(R.id.tvSelectedApp)

        etMinPrice.setText(prefs.getFloat("min_price", 0f).toString())
        etMaxDistancePickup.setText(prefs.getFloat("max_pickup_km", 5f).toString())
        etMinPricePerKm.setText(prefs.getFloat("min_price_per_km", 0f).toString())
        etMaxRideDistance.setText(prefs.getFloat("max_ride_km", 999f).toString())
        switchAvoidLowRated.isChecked = prefs.getBoolean("avoid_low_rated", false)

        val selectedPkg = prefs.getString("target_package", null)
        tvSelectedApp.text = selectedPkg?.let { "Application actuelle : ${getAppLabel(it)}" }
            ?: "Aucune application sélectionnée"

        btnSelectApp.setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            prefs.edit().apply {
                putFloat("min_price", etMinPrice.text.toString().toFloatOrNull() ?: 0f)
                putFloat("max_pickup_km", etMaxDistancePickup.text.toString().toFloatOrNull() ?: 5f)
                putFloat("min_price_per_km", etMinPricePerKm.text.toString().toFloatOrNull() ?: 0f)
                putFloat("max_ride_km", etMaxRideDistance.text.toString().toFloatOrNull() ?: 999f)
                putBoolean("avoid_low_rated", switchAvoidLowRated.isChecked)
                apply()
            }
            Toast.makeText(this, "Conditions enregistrées", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        val selectedPkg = prefs.getString("target_package", null)
        findViewById<TextView>(R.id.tvSelectedApp).text = selectedPkg?.let {
            "Application actuelle : ${getAppLabel(it)}"
        } ?: "Aucune application sélectionnée"
    }

    private fun getAppLabel(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }
}
