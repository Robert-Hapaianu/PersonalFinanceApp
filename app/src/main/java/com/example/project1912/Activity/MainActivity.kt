package com.example.project1912.Activity

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project1912.Adapter.ExpenseListAdapter
import com.example.project1912.ViewModel.MainViewModel
import com.example.project1912.databinding.ActivityMainBinding
import eightbitlab.com.blurview.RenderScriptBlur

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
    private val PERMISSION_REQUEST_CODE = 123
    private val PREFS_NAME = "ProfilePrefs"
    private val PROFILE_IMAGE_URI = "profile_image_uri"
    private val USER_NAME = "user_name"
    private val USER_EMAIL = "user_email"

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                binding.profileImageView.setImageURI(uri)
                saveProfileImageUri(uri.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        initRecyclerview()
        setBlueEffect()
        setVariable()
        setupProfileImage()
        setupEditButtons()
        loadSavedProfileImage()
        loadSavedUserInfo()
    }

    private fun setupEditButtons() {
        binding.nameEditButton.setOnClickListener {
            showEditDialog(true)
        }

        binding.emailEditButton.setOnClickListener {
            showEditDialog(false)
        }
    }

    private fun showEditDialog(isName: Boolean) {
        val editText = EditText(this).apply {
            setText(if (isName) binding.textView.text else binding.textView3.text)
            if (!isName) {
                inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }
        }

        AlertDialog.Builder(this)
            .setTitle(if (isName) "Edit Name" else "Edit Email")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString()
                if (isName) {
                    binding.textView.text = newText
                } else {
                    binding.textView3.text = newText
                }
                saveUserInfo()
            }
            .setNegativeButton("Cancel", null)
            .show()

        // Show keyboard automatically
        editText.requestFocus()
    }

    private fun saveUserInfo() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putString(USER_NAME, binding.textView.text.toString())
            putString(USER_EMAIL, binding.textView3.text.toString())
            apply()
        }
    }

    private fun loadSavedUserInfo() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedName = sharedPreferences.getString(USER_NAME, null)
        val savedEmail = sharedPreferences.getString(USER_EMAIL, null)

        if (savedName != null) {
            binding.textView.text = savedName
        }
        if (savedEmail != null) {
            binding.textView3.text = savedEmail
        }
    }

    private fun saveProfileImageUri(uriString: String) {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putString(PROFILE_IMAGE_URI, uriString)
            apply()
        }
    }

    private fun loadSavedProfileImage() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedImageUri = sharedPreferences.getString(PROFILE_IMAGE_URI, null)
        
        if (savedImageUri != null) {
            try {
                val uri = Uri.parse(savedImageUri)
                // Verify the URI is still valid and accessible
                contentResolver.openInputStream(uri)?.use {
                    binding.profileImageView.setImageURI(uri)
                }
            } catch (e: Exception) {
                // If there's any error loading the saved image, revert to default
                binding.profileImageView.setImageResource(com.example.project1912.R.drawable.men)
                // Clear the invalid URI from SharedPreferences
                saveProfileImageUri("")
            }
        }
    }

    private fun setupProfileImage() {
        binding.profileImageView.setOnClickListener {
            if (checkAndRequestPermissions()) {
                openGallery()
            }
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    PERMISSION_REQUEST_CODE
                )
                return false
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }

    private fun setVariable() {
        binding.cardBtn.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ReportActivity::class.java
                )
            )
        }
    }

    private fun setBlueEffect() {
        val radius = 10f
        val decorView = this.window.decorView
        val rootView = decorView.findViewById<View>(android.R.id.content) as ViewGroup
        val windowBackground = decorView.background
        binding.blueView.setupWith(
            rootView,
            RenderScriptBlur(this)
        )
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)

        binding.blueView.setOutlineProvider(ViewOutlineProvider.BACKGROUND)
        binding.blueView.setClipToOutline(true)
    }

    private fun initRecyclerview() {
        binding.view1.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.view1.adapter = ExpenseListAdapter(mainViewModel.loadData())
        binding.view1.isNestedScrollingEnabled = false
    }
}