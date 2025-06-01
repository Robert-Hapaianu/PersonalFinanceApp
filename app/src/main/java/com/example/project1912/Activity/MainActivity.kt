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
import com.example.project1912.Utils.SecureTokenStorage
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
import kotlinx.coroutines.delay

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
    private lateinit var secureTokenStorage: SecureTokenStorage

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
        secureTokenStorage = SecureTokenStorage(this)

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
                        pic = "btn_1",  // Use btn_1.png image for all transactions
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
        updateTotalBalance()
    }

    private fun updateTotalBalance() {
        val totalBalance = cardAdapter.getTotalBalance()
        binding.textView7.text = String.format("%.2f lei", totalBalance)
    }

    fun removeTransactionsForCard(cardId: String) {
        expenseAdapter.removeTransactionsByCardId(cardId)
    }

    fun showAddCardDialog() {
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
                    updateTotalBalance()
                    
                    // Generate access token with the card ID
                    generateAccessToken(newCard.cardNumber)
                } else {
                    Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()

        // Show keyboard automatically
        cardNumberEdit.requestFocus()
    }

    private fun generateAccessToken(cardId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("https://bankaccountdata.gocardless.com/api/v2/token/new/")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val requestBody = JSONObject().apply {
                    put("secret_id", "7d4b9dc9-7d40-48b8-85c2-b3c0371d85ff")
                    put("secret_key", "8f289965b85183a5d5dc1357595f7e3ef0fb43bc55cb823acdad633084193c3439651adbbff7411b4473a6bb84188f2f44584107d2818177fa16c0ebce6105b6")
                }.toString()

                println("Request Body: $requestBody")

                connection.outputStream.use { os ->
                    os.write(requestBody.toByteArray())
                    os.flush()
                }

                val responseCode = connection.responseCode
                println("Token Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    println("Token Response: $response")
                    
                    val jsonResponse = JSONObject(response)
                    val accessToken = jsonResponse.getString("access")
                    val refreshToken = jsonResponse.getString("refresh")
                    
                    secureTokenStorage.saveAccessToken(accessToken)
                    secureTokenStorage.saveRefreshToken(refreshToken)
                    
                    // Create requisition after getting the access token
                    createRequisition(accessToken, cardId)
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    println("Token Error Response: $errorResponse")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Failed to generate tokens: $responseCode - $errorResponse", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                println("Token Error: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun createRequisition(accessToken: String, cardId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("https://bankaccountdata.gocardless.com/api/v2/requisitions/")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val requestBody = JSONObject().apply {
                    put("redirect", "myapp://callback")
                    put("institution_id", "BRD_GROUPE_SOCIETE_GENERALE_RO_BRDEROBU")
                    put("reference", java.util.UUID.randomUUID().toString())
                }.toString()

                println("Requisition Request URL: ${url.toString()}")
                println("Requisition Request Headers: ${connection.requestProperties}")
                println("Requisition Request Body: $requestBody")

                connection.outputStream.use { os ->
                    os.write(requestBody.toByteArray())
                    os.flush()
                }

                val responseCode = connection.responseCode
                println("Requisition Response Code: $responseCode")

                val responseBody = try {
                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                        connection.inputStream.bufferedReader().use { it.readText() }
                    } else {
                        connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message available"
                    }
                } catch (e: Exception) {
                    "Error reading response: ${e.message}"
                }

                println("Requisition Response Body: $responseBody")

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val requisitionId = jsonResponse.getString("id")
                        val link = jsonResponse.getString("link")
                        
                        secureTokenStorage.saveRequisitionId(requisitionId)
                        
                        withContext(Dispatchers.Main) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse(link)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                }
                                startActivity(intent)
                            } catch (e: Exception) {
                                println("Browser Error: ${e.message}")
                                e.printStackTrace()
                                Toast.makeText(this@MainActivity, "Could not open browser: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }

                        // Wait for a short time to ensure the user has completed the bank authentication
                        delay(5000)

                        // Get the requisition details
                        val requisitionDetails = getRequisitionDetails(accessToken, requisitionId)
                        if (requisitionDetails != null) {
                            // Get the account balance
                            getAccountBalance(accessToken, requisitionDetails, cardId)
                        }
                    } catch (e: Exception) {
                        println("JSON Parsing Error: ${e.message}")
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Error parsing response: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val errorMessage = "Failed to create requisition: $responseCode - $responseBody"
                        println(errorMessage)
                        Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                println("Requisition Error: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private suspend fun getRequisitionDetails(accessToken: String, requisitionId: String): String? {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://bankaccountdata.gocardless.com/api/v2/requisitions/$requisitionId/")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode
            println("Requisition Details Response Code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                println("Requisition Details Response: $responseBody")

                val jsonResponse = JSONObject(responseBody)
                val status = jsonResponse.getString("status")
                println("Requisition Status: $status")

                if (status == "LN") {
                    val accountsArray = jsonResponse.getJSONArray("accounts")
                    
                    if (accountsArray.length() > 0) {
                        val accountId = accountsArray.getString(0)
                        secureTokenStorage.saveAccountId(accountId)
                        return accountId
                    }
                } else {
                    println("Waiting for requisition status to be LN. Current status: $status")
                    // Wait for 5 seconds before checking again
                    delay(5000)
                    return getRequisitionDetails(accessToken, requisitionId)
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                println("Requisition Details Error: $errorResponse")
            }
        } catch (e: Exception) {
            println("Requisition Details Error: ${e.message}")
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        return null
    }

    private suspend fun getAccountBalance(accessToken: String, accountId: String, cardId: String) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://bankaccountdata.gocardless.com/api/v2/accounts/$accountId/balances/")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode
            println("Account Balance Response Code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                println("Account Balance Response: $responseBody")

                val jsonResponse = JSONObject(responseBody)
                val balancesArray = jsonResponse.getJSONArray("balances")
                
                if (balancesArray.length() > 0) {
                    val firstBalance = balancesArray.getJSONObject(0)
                    val balanceAmount = firstBalance.getJSONObject("balanceAmount")
                    val amount = balanceAmount.getString("amount")
                    
                    // Format the amount with 2 decimal places
                    val formattedAmount = String.format("%.2f", amount.toDouble())
                    val balanceText = "$formattedAmount lei"
                    
                    // Save the balance for this specific card
                    secureTokenStorage.saveBankBalance(cardId, balanceText)
                    
                    // Update the card's balance in the adapter
                    withContext(Dispatchers.Main) {
                        cardAdapter.updateCardBalance(cardId, amount.toDouble())
                        updateTotalBalance()
                    }

                    // Update the balance in Report Activity
                    withContext(Dispatchers.Main) {
                        val intent = Intent(this@MainActivity, ReportActivity::class.java).apply {
                            putExtra("BALANCE", balanceText)
                            putExtra("CARD_ID", cardId)
                        }
                        startActivity(intent)
                    }

                    // Fetch and add transactions after getting balance
                    fetchAndAddTransactions(accessToken, accountId, cardId)
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                println("Account Balance Error: $errorResponse")
            }
        } catch (e: Exception) {
            println("Account Balance Error: ${e.message}")
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun fetchAndAddTransactions(accessToken: String, accountId: String, cardId: String) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://bankaccountdata.gocardless.com/api/v2/accounts/$accountId/transactions/")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode
            println("Transactions Response Code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                println("Transactions Response: $responseBody")

                val jsonResponse = JSONObject(responseBody)
                val transactions = jsonResponse.getJSONObject("transactions")
                val bookedTransactions = transactions.getJSONArray("booked")

                // Get current month and year
                val calendar = java.util.Calendar.getInstance()
                val currentMonth = calendar.get(java.util.Calendar.MONTH)
                val currentYear = calendar.get(java.util.Calendar.YEAR)

                // Variables to track totals
                var totalIncome = 0.0
                var totalExpense = 0.0

                println("Starting transaction processing...")
                println("Current month: $currentMonth, Current year: $currentYear")

                // Process each transaction
                for (i in 0 until bookedTransactions.length()) {
                    val transaction = bookedTransactions.getJSONObject(i)
                    
                    // Skip if no creditorName
                    if (!transaction.has("creditorName")) {
                        continue
                    }
                    
                    val bookingDate = transaction.getString("bookingDate")
                    val creditorName = transaction.getString("creditorName")
                    val amount = transaction.getJSONObject("transactionAmount")
                        .getString("amount")
                        .toDouble() // Keep the sign to determine if it's income or expense

                    println("Processing transaction: $creditorName, Amount: $amount, Date: $bookingDate")

                    // Parse the booking date
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val date = dateFormat.parse(bookingDate)
                    val transactionCalendar = java.util.Calendar.getInstance()
                    transactionCalendar.time = date

                    // Check if transaction is from current month
                    if (transactionCalendar.get(java.util.Calendar.MONTH) == currentMonth &&
                        transactionCalendar.get(java.util.Calendar.YEAR) == currentYear) {
                        
                        // Add to appropriate total
                        if (amount > 0) {
                            totalIncome += amount
                            println("Added to income: $amount, New total income: $totalIncome")
                        } else {
                            totalExpense += -amount // Convert negative to positive for expense total
                            println("Added to expense: ${-amount}, New total expense: $totalExpense")
                        }

                        // Format the date for display
                        val displayDateFormat = java.text.SimpleDateFormat("HH:mm • dd MMM yyyy", java.util.Locale.getDefault())
                        val formattedDate = displayDateFormat.format(date)

                        // Create and add the expense
                        val newExpense = ExpenseDomain(
                            title = creditorName,
                            price = -amount, // Convert to positive for display
                            pic = "btn_1", // Use btn_1.png image for all transactions
                            time = formattedDate,
                            cardId = cardId // Associate with the specific card
                        )

                        withContext(Dispatchers.Main) {
                            expenseAdapter.addItem(newExpense)
                            expenseAdapter.saveExpenses()
                        }
                    } else {
                        println("Skipping transaction from different month/year")
                    }
                }

                // Format totals with 2 decimal places
                val formattedIncome = String.format("%.2f", totalIncome)
                val formattedExpense = String.format("%.2f", totalExpense)

                println("Final totals:")
                println("Total Income: $formattedIncome lei")
                println("Total Expense: $formattedExpense lei")

                // Update ReportActivity with the totals
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@MainActivity, ReportActivity::class.java).apply {
                        putExtra("INCOME", "$formattedIncome lei")
                        putExtra("EXPENSE", "$formattedExpense lei")
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // Clear any existing instances
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                println("Transactions Error Response: $errorResponse")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to fetch transactions: $responseCode", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            println("Transactions Error: ${e.message}")
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Error fetching transactions: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } finally {
            connection?.disconnect()
        }
    }
}