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
import android.text.InputFilter
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
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
import com.example.project1912.Adapter.CardAdapter
import com.example.project1912.Adapter.ExpenseListAdapter
import com.example.project1912.Domain.CardDomain
import com.example.project1912.Domain.ExpenseDomain
import com.example.project1912.ViewModel.MainViewModel
import com.example.project1912.databinding.ActivityMainBinding
import eightbitlab.com.blurview.RenderScriptBlur
import android.widget.ArrayAdapter
import android.text.Editable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
    private val PERMISSION_REQUEST_CODE = 123
    private val PREFS_NAME = "ProfilePrefs"
    private val PROFILE_IMAGE_URI = "profile_image_uri"
    private val USER_NAME = "user_name"
    private val USER_EMAIL = "user_email"
    private lateinit var expenseAdapter: ExpenseListAdapter
    private lateinit var cardAdapter: CardAdapter
    private val cards = mutableListOf(
        CardDomain(
            cardNumber = "1234 5678 9012 3456",
            expiryDate = "03/07",
            cardType = "visa",
            balance = 23451.58
        )
    )

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
        initCardView()
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
        // Card click handling is now done in the CardAdapter
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

    private fun initCardView() {
        // Load saved cards or use default data if none exists
        val savedCards = CardAdapter.loadSavedCards(this)
        cardAdapter = CardAdapter(savedCards, this)
        binding.cardsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = cardAdapter
        }
        
        binding.addCardBtn.setOnClickListener {
            showAddCardDialog()
        }
    }

    private fun showAddCardDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        val cardNumberLabel = TextView(this).apply {
            text = "Card Number"
            setTextColor(ContextCompat.getColor(this@MainActivity, com.example.project1912.R.color.darkblue))
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }
        layout.addView(cardNumberLabel)

        val cardNumberEdit = EditText(this).apply {
            hint = "Enter card number"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO
            filters = arrayOf(InputFilter.LengthFilter(19)) // 16 digits + 3 spaces
            
            // Add text change listener to format card number
            addTextChangedListener(object : android.text.TextWatcher {
                private var isFormatting = false
                
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                
                override fun afterTextChanged(s: Editable?) {
                    if (isFormatting) return
                    isFormatting = true
                    
                    val text = s.toString().replace(" ", "")
                    val formatted = StringBuilder()
                    
                    for (i in text.indices) {
                        if (i > 0 && i % 4 == 0) {
                            formatted.append(" ")
                        }
                        formatted.append(text[i])
                    }
                    
                    s?.replace(0, s.length, formatted.toString())
                    isFormatting = false
                }
            })
        }
        layout.addView(cardNumberEdit)

        val spacing1 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                20
            )
        }
        layout.addView(spacing1)

        val expiryLabel = TextView(this).apply {
            text = "Expiry Date (MM/YY)"
            setTextColor(ContextCompat.getColor(this@MainActivity, com.example.project1912.R.color.darkblue))
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }
        layout.addView(expiryLabel)

        val expiryEdit = EditText(this).apply {
            hint = "MM/YY"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO
            filters = arrayOf(InputFilter.LengthFilter(5)) // MM/YY format
            
            // Add text change listener to format expiry date
            addTextChangedListener(object : android.text.TextWatcher {
                private var isFormatting = false
                
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                
                override fun afterTextChanged(s: Editable?) {
                    if (isFormatting) return
                    isFormatting = true
                    
                    val text = s.toString().replace("/", "")
                    if (text.length > 2) {
                        val formatted = "${text.substring(0, 2)}/${text.substring(2)}"
                        s?.clear()
                        s?.append(formatted)
                    } else if (text.length == 2) {
                        s?.clear()
                        s?.append("$text/")
                    }
                    
                    isFormatting = false
                }
            })
        }
        layout.addView(expiryEdit)

        val spacing2 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                20
            )
        }
        layout.addView(spacing2)

        val bankLabel = TextView(this).apply {
            text = "Bank"
            setTextColor(ContextCompat.getColor(this@MainActivity, com.example.project1912.R.color.darkblue))
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }
        layout.addView(bankLabel)

        val bankSpinner = Spinner(this).apply {
            val adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                arrayOf("BRD")
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            this.adapter = adapter
        }
        layout.addView(bankSpinner)

        AlertDialog.Builder(this)
            .setTitle("Add New Card")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val cardNumber = cardNumberEdit.text.toString().replace(" ", "")
                val expiryDate = expiryEdit.text.toString()
                val bank = bankSpinner.selectedItem.toString()

                if (cardNumber.length == 16 && expiryDate.matches(Regex("\\d{2}/\\d{2}"))) {
                    // Create and add the card domain
                    val newCard = CardDomain(
                        cardNumber = cardNumber.chunked(4).joinToString(" "),
                        expiryDate = expiryDate,
                        cardType = bank,
                        balance = 0.0
                    )
                    cardAdapter.addCard(newCard)
                    
                    // Generate access token
                    generateAccessToken()
                } else {
                    Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateAccessToken() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://bankaccountdata.gocardless.com/api/v2/token/new/")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val requestBody = JSONObject().apply {
                    put("secret_id", "7d4b9dc9-7d40-48b8-85c2-b3c0371d85ff")
                    put("secret_key", "8f289965b85183a5d5dc1357595f7e3ef0fb43bc55cb823acdad633084193c3439651adbbff7411b4473a6bb84188f2f44584107d2818177fa16c0ebce6105b6")
                }.toString()

                connection.outputStream.use { os ->
                    os.write(requestBody.toByteArray())
                    os.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(response)
                    
                    val accessToken = jsonResponse.getString("access")
                    val refreshToken = jsonResponse.getString("refresh")
                    
                    withContext(Dispatchers.Main) {
                        println("Access Token: $accessToken")
                        println("Refresh Token: $refreshToken")
                        Toast.makeText(this@MainActivity, "Tokens generated successfully", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Failed to generate tokens", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}