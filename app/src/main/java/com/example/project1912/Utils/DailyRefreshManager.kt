package com.example.project1912.Utils

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.example.project1912.Adapter.CardAdapter
import com.example.project1912.Adapter.ExpenseListAdapter
import com.example.project1912.Domain.ExpenseDomain
import java.util.*

class DailyRefreshManager(private val context: Context) {
    
    private val secureTokenStorage = SecureTokenStorage(context)
    
    /**
     * Main function to refresh all data for all cards
     */
    fun performDailyRefresh() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                println("Starting daily refresh at midnight...")
                
                // Step 1: Refresh the access token
                val newAccessToken = refreshAccessToken()
                if (newAccessToken == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to refresh access token", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                
                // Step 2: Get all saved cards
                val savedCards = CardAdapter.loadSavedCards(context)
                println("Found ${savedCards.size} cards to refresh")
                
                // Step 3: Refresh balance and transactions for each card
                for (card in savedCards) {
                    val accountId = secureTokenStorage.getAccountIdForCard(card.cardNumber)
                    if (accountId != null) {
                        println("Refreshing data for card: ${card.cardNumber}")
                        
                        // Refresh balance
                        refreshCardBalance(newAccessToken, accountId, card.cardNumber)
                        
                        // Refresh transactions (only new ones from current month)
                        refreshCardTransactions(newAccessToken, accountId, card.cardNumber)
                    } else {
                        println("No account ID found for card: ${card.cardNumber}")
                    }
                }
                
                // Update UI on the main thread if the activity is available
                withContext(Dispatchers.Main) {
                    try {
                        // Try to update UI if MainActivity is available
                        val activity = context as? com.example.project1912.Activity.MainActivity
                        activity?.refreshCardBalancesFromStorage()
                        activity?.refreshExpenseListFromStorage()
                        println("UI updated with refreshed balances and transactions")
                    } catch (e: Exception) {
                        println("Could not update UI: ${e.message}")
                    }
                }
                
                println("Daily refresh completed successfully")
                
            } catch (e: Exception) {
                println("Error during daily refresh: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Refresh the access token using the refresh token
     */
    private suspend fun refreshAccessToken(): String? {
        var connection: HttpURLConnection? = null
        try {
            val refreshToken = secureTokenStorage.getRefreshToken()
            if (refreshToken == null) {
                println("No refresh token available")
                return null
            }
            
            val url = URL("https://bankaccountdata.gocardless.com/api/v2/token/refresh/")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            val requestBody = JSONObject().apply {
                put("refresh", refreshToken)
            }.toString()
            
            println("Refreshing access token...")
            
            connection.outputStream.use { os ->
                os.write(requestBody.toByteArray())
                os.flush()
            }
            
            val responseCode = connection.responseCode
            println("Token refresh response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                println("Token refresh response: $response")
                
                val jsonResponse = JSONObject(response)
                val newAccessToken = jsonResponse.getString("access")
                
                // Save the new access token
                secureTokenStorage.saveAccessToken(newAccessToken)
                println("Access token refreshed successfully")
                
                return newAccessToken
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                println("Token refresh error: $errorResponse")
                return null
            }
        } catch (e: Exception) {
            println("Error refreshing token: ${e.message}")
            e.printStackTrace()
            return null
        } finally {
            connection?.disconnect()
        }
    }
    
    /**
     * Refresh balance for a specific card
     */
    private suspend fun refreshCardBalance(accessToken: String, accountId: String, cardId: String) {
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
            println("Balance refresh response code for card $cardId: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                println("Balance refresh response for card $cardId: $responseBody")
                
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
                    
                    println("Updated balance for card $cardId: $balanceText")
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                println("Balance refresh error for card $cardId: $errorResponse")
            }
        } catch (e: Exception) {
            println("Error refreshing balance for card $cardId: ${e.message}")
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
    }
    
    /**
     * Refresh transactions for a specific card (only add new transactions from current month)
     */
    private suspend fun refreshCardTransactions(accessToken: String, accountId: String, cardId: String) {
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
            println("Transactions refresh response code for card $cardId: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                println("Transactions refresh response for card $cardId: $responseBody")
                
                val jsonResponse = JSONObject(responseBody)
                val transactions = jsonResponse.getJSONObject("transactions")
                val bookedTransactions = transactions.getJSONArray("booked")
                
                // Get current month and year
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)
                
                // Load existing expenses to check for duplicates
                val existingExpenses = ExpenseListAdapter.loadSavedExpenses(context)
                
                // Variables to track totals
                var totalIncome = 0.0
                var totalExpense = 0.0
                var newTransactionCount = 0
                
                println("Processing transactions for card $cardId...")
                println("Current month: $currentMonth, Current year: $currentYear")
                
                // Process each transaction
                for (i in 0 until bookedTransactions.length()) {
                    val transaction = bookedTransactions.getJSONObject(i)
                    
                    val bookingDate = transaction.getString("bookingDate")
                    val amount = transaction.getJSONObject("transactionAmount")
                        .getString("amount")
                        .toDouble()
                    
                    // Parse the booking date to check if it's from current month
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = dateFormat.parse(bookingDate)
                    val transactionCalendar = Calendar.getInstance()
                    
                    if (date != null) {
                        transactionCalendar.time = date
                        
                        // Check if transaction is from current month
                        if (transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                            transactionCalendar.get(Calendar.YEAR) == currentYear) {
                            
                            // Add to appropriate total based on amount sign
                            if (amount > 0) {
                                totalIncome += amount
                            } else {
                                totalExpense += -amount
                            }
                            
                            // Only process negative transactions with creditorName for display
                            if (amount < 0 && transaction.has("creditorName")) {
                                val creditorName = transaction.getString("creditorName")
                                
                                // Check if this transaction already exists
                                val transactionExists = existingExpenses.any { expense ->
                                    expense.title == creditorName &&
                                    expense.price == -amount &&
                                    expense.cardId == cardId &&
                                    expense.time.contains(bookingDate.substring(8, 10)) // Check day
                                }
                                
                                if (!transactionExists) {
                                    // Format the date for display
                                    val displayDateFormat = java.text.SimpleDateFormat("HH:mm • dd MMM yyyy", Locale.getDefault())
                                    val formattedDate = displayDateFormat.format(date)
                                    
                                    // Auto-assign budget based on creditor name
                                    val autoAssignedBudget = ExpenseListAdapter.autoAssignBudget(context, creditorName)
                                    
                                    // Create the new expense
                                    val newExpense = ExpenseDomain(
                                        title = creditorName,
                                        price = -amount,
                                        pic = "btn_1",
                                        time = formattedDate,
                                        cardId = cardId,
                                        budget = autoAssignedBudget
                                    )
                                    
                                    // Add to existing expenses list and save
                                    existingExpenses.add(newExpense)
                                    newTransactionCount++
                                    
                                    println("Added new transaction: $creditorName, Amount: ${-amount}, Date: $formattedDate")
                                } else {
                                    println("Transaction already exists: $creditorName")
                                }
                            }
                        }
                    }
                }
                
                // Save updated expenses list
                if (newTransactionCount > 0) {
                    val sharedPreferences = context.getSharedPreferences("ExpensesPrefs", Context.MODE_PRIVATE)
                    val gson = com.google.gson.Gson()
                    val json = gson.toJson(existingExpenses)
                    sharedPreferences.edit().putString("expenses", json).apply()
                    println("Added $newTransactionCount new transactions for card $cardId")
                }
                
                // Update income and expense totals for this card
                val formattedIncome = String.format("%.2f", totalIncome)
                val formattedExpense = String.format("%.2f", totalExpense)
                
                secureTokenStorage.saveIncomeForCard(cardId, "$formattedIncome lei")
                secureTokenStorage.saveExpenseForCard(cardId, "$formattedExpense lei")
                
                println("Updated totals for card $cardId - Income: $formattedIncome lei, Expense: $formattedExpense lei")
                
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                println("Transactions refresh error for card $cardId: $errorResponse")
            }
        } catch (e: Exception) {
            println("Error refreshing transactions for card $cardId: ${e.message}")
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
    }
} 