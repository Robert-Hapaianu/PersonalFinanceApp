package com.example.project1912.Activity

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project1912.Adapter.ExpenseListAdapter
import com.example.project1912.Domain.ExpenseDomain
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
    private lateinit var expenseAdapter: ExpenseListAdapter

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

        // Ensure we start at the top of the screen
        binding.scrollView2.post {
            binding.scrollView2.scrollTo(0, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset scroll position when returning to the activity
        binding.scrollView2.post {
            binding.scrollView2.scrollTo(0, 0)
        }
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
        // Load saved expenses or use default data if none exists
        val savedExpenses = ExpenseListAdapter.loadSavedExpenses(this)
        expenseAdapter = if (savedExpenses.isNotEmpty()) {
            ExpenseListAdapter(savedExpenses)
        } else {
            ExpenseListAdapter(mainViewModel.loadData())
        }
        binding.view1.adapter = expenseAdapter
        binding.view1.isNestedScrollingEnabled = true // Enable nested scrolling

        setupAddExpenseFab()

        // Add swipe-to-delete functionality
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                expenseAdapter.removeItem(position)
                expenseAdapter.saveExpenses() // Save changes after deletion
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

                val itemView = viewHolder.itemView
                val background = ColorDrawable(Color.RED)

                if (dX > 0) { // Swiping to the right
                    background.setBounds(
                        itemView.left,
                        itemView.top,
                        itemView.left + dX.toInt(),
                        itemView.bottom
                    )
                } else if (dX < 0) { // Swiping to the left
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                } else {
                    background.setBounds(0, 0, 0, 0)
                }

                background.draw(c)
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.view1)
    }

    private fun setupAddExpenseFab() {
        binding.addExpenseFab.setOnClickListener {
            showAddExpenseDialog()
        }
    }

    private fun showAddExpenseDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        val activityLabel = TextView(this).apply {
            text = "Activity"
            setTextColor(ContextCompat.getColor(this@MainActivity, com.example.project1912.R.color.darkblue))
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }
        layout.addView(activityLabel)

        val activityEdit = EditText(this).apply {
            hint = "Enter activity name"
        }
        layout.addView(activityEdit)

        val spacing = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                20
            )
        }
        layout.addView(spacing)

        val priceLabel = TextView(this).apply {
            text = "Price"
            setTextColor(ContextCompat.getColor(this@MainActivity, com.example.project1912.R.color.darkblue))
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }
        layout.addView(priceLabel)

        val priceEdit = EditText(this).apply {
            hint = "Enter price"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or 
                       android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        layout.addView(priceEdit)

        AlertDialog.Builder(this)
            .setTitle("Add New Expense")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val title = activityEdit.text.toString()
                val price = priceEdit.text.toString().toDoubleOrNull()

                if (title.isNotEmpty() && price != null) {
                    val currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date())
                    val currentDate = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date())
                    
                    val newExpense = ExpenseDomain(
                        title = title,
                        price = price,
                        pic = "restaurant",  // Default icon for new entries
                        time = "$currentTime • $currentDate"
                    )
                    
                    expenseAdapter.addItem(newExpense)
                    expenseAdapter.saveExpenses()
                } else {
                    Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()

        // Show keyboard automatically
        activityEdit.requestFocus()
    }
}