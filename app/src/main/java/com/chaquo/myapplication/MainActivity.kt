package com.chaquo.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : AppCompatActivity() {

    // Request code for location permission
    private val REQUEST_PERMISSION_ACCESS_FINE_LOCATION = 1
    // Hardcoded address of the Empatica E4 device
    private val EMPATICA_E4_ADDRESS = "2C:11:65:5A:6F:33"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start the Python interpreter if it is not already started
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        // Check for necessary permissions and initialize the application
        checkPermissionsAndInitialize()

        // Set up the connect button click listener
        findViewById<Button>(R.id.connectButton).setOnClickListener {
            try {
                val statusLabel = findViewById<TextView>(R.id.status_label)

                // Update the status label to indicate that the process is running
                statusLabel.text = "Status: Running..."

                // Call the Python function to process data
                val result = callPythonProcessData(EMPATICA_E4_ADDRESS)

                // Update the status label with the result
                statusLabel.text = "Status: $result"

                // Show a toast message with the result
                Toast.makeText(this, result, Toast.LENGTH_LONG).show()

                // Hide the soft keyboard if it is open
                currentFocus?.let { view ->
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
            } catch (e: PyException) {
                // Handle any Python exceptions and update the status label with the error message
                findViewById<TextView>(R.id.status_label).text = "Status: Error - ${e.message}"
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Function to check for necessary permissions and initialize the application
    private fun checkPermissionsAndInitialize() {
        // Check if the required permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

            // Request the necessary permissions
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN), REQUEST_PERMISSION_ACCESS_FINE_LOCATION)
        } else {
            // Permissions are granted, proceed with initializing Bluetooth or other tasks
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSION_ACCESS_FINE_LOCATION) {
            // Check if all requested permissions were granted
            val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allPermissionsGranted) {
                // Permissions were granted, proceed with initializing Bluetooth or other tasks
            } else {
                // Handle the case where permissions were not granted
                showPermissionsRequiredDialog()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    // Show a dialog to inform the user that permissions are required
    private fun showPermissionsRequiredDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Permission required")
        builder.setMessage("Without these permissions, the app cannot find Bluetooth low energy devices. Please allow them.")
        builder.setPositiveButton("Retry") { dialog, _ ->
            checkPermissionsAndInitialize()
        }
        builder.setNegativeButton("Exit application") { dialog, _ ->
            finish()
        }
        builder.setNeutralButton("Settings") { _, _ ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
        builder.show()
    }

    // Function to call the Python script to process data
    private fun callPythonProcessData(serverAddress: String): String {
        val py = Python.getInstance()
        val module = py.getModule("empatica_script")
        return module.callAttr("process_data", serverAddress).toString()
    }
}
