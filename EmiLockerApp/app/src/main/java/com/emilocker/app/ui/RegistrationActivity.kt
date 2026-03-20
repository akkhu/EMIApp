package com.emilocker.app.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.emilocker.app.DeviceRestrictionManager
import com.emilocker.app.R
import com.emilocker.app.api.ApiClient
import com.emilocker.app.api.models.MobileRegisterRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegistrationActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    private lateinit var etCustomerName: EditText
    private lateinit var etDob: EditText
    private lateinit var etAadhaarNumber: EditText
    private lateinit var etPanNumber: EditText
    private lateinit var etFatherName: EditText
    private lateinit var etVillage: EditText
    private lateinit var spClient: Spinner
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        etCustomerName = findViewById(R.id.etCustomerName)
        etDob = findViewById(R.id.etDob)
        etAadhaarNumber = findViewById(R.id.etAadhaarNumber)
        etPanNumber = findViewById(R.id.etPanNumber)
        etFatherName = findViewById(R.id.etFatherName)
        etVillage = findViewById(R.id.etVillage)
        spClient = findViewById(R.id.spClient)
        btnSave = findViewById(R.id.btnSave)

        // Setup Spinner
        val clients = arrayOf("Select a Client", "test1", "test2", "test3")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, clients)
        spClient.adapter = adapter

        setupValidation()

        btnSave.setOnClickListener {
            if (spClient.selectedItemPosition == 0) {
                Toast.makeText(this, "Please select a valid client", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            registerDevice()
        }
    }

    private fun setupValidation() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateForm()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        etCustomerName.addTextChangedListener(textWatcher)
        etDob.addTextChangedListener(textWatcher)
        etAadhaarNumber.addTextChangedListener(textWatcher)
        etPanNumber.addTextChangedListener(textWatcher)
        etFatherName.addTextChangedListener(textWatcher)
        etVillage.addTextChangedListener(textWatcher)
    }

    private fun validateForm() {
        val isValid = etCustomerName.text.isNotBlank() &&
                etDob.text.isNotBlank() &&
                etAadhaarNumber.text.isNotBlank() &&
                etPanNumber.text.isNotBlank() &&
                etFatherName.text.isNotBlank() &&
                etVillage.text.isNotBlank()
        
        btnSave.isEnabled = isValid
    }

    private fun registerDevice() {
        btnSave.isEnabled = false
        btnSave.text = "Registering..."

        scope.launch {
            try {
                val masterKey = MasterKey.Builder(applicationContext, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                val sharedPrefs = EncryptedSharedPreferences.create(
                    applicationContext,
                    "emi_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )

                val appId = sharedPrefs.getString("appId", "EMI_PRO")

                if (appId == null) {
                    Toast.makeText(this@RegistrationActivity, "Missing App ID. Factory reset and scan QR again.", Toast.LENGTH_LONG).show()
                    btnSave.isEnabled = true
                    btnSave.text = "Save & Register Device"
                    return@launch
                }

                val enteredAadhaar = etAadhaarNumber.text.toString().trim()

                val request = MobileRegisterRequest(
                    appId = appId,
                    customerName = etCustomerName.text.toString().trim(),
                    dob = etDob.text.toString().trim(),
                    aadhaarNumber = enteredAadhaar,
                    panNumber = etPanNumber.text.toString().trim(),
                    fatherName = etFatherName.text.toString().trim(),
                    village = etVillage.text.toString().trim(),
                    clientName = spClient.selectedItem.toString()
                )

                val response = withContext(Dispatchers.IO) {
                    ApiClient.getService().registerDevice(request)
                }

                if (response.isSuccessful) {
                    // Save Aadhaar Number as the primary locking ID for future requests
                    sharedPrefs.edit().putString("aadhaarNumber", enteredAadhaar).apply()

                    Toast.makeText(this@RegistrationActivity, "Registration Successful!", Toast.LENGTH_LONG).show()
                    
                    // Hide app and enforce lockdown
                    val manager = DeviceRestrictionManager(applicationContext)
                    manager.hideAppFromLauncher()
                    manager.enforceEmiLock() // Keep the device locked until unlocked from Dashboard
                    
                    finish()
                } else {
                    Toast.makeText(this@RegistrationActivity, "Server Error: ${response.code()}", Toast.LENGTH_LONG).show()
                    btnSave.isEnabled = true
                    btnSave.text = "Save & Register Device"
                }

            } catch (e: Exception) {
                Toast.makeText(this@RegistrationActivity, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                btnSave.isEnabled = true
                btnSave.text = "Save & Register Device"
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent backing out of registration
        Toast.makeText(this, "Please complete registration", Toast.LENGTH_SHORT).show()
    }
}
