package com.example.project1912.Adapter

import android.content.Context
import android.icu.text.DecimalFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.project1912.Domain.MonthlyHistoryDomain
import com.example.project1912.Domain.ExpenseDomain
import com.example.project1912.Domain.CardDomain
import com.example.project1912.databinding.ViewholderMonthlyHistoryBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class MonthlyHistoryAdapter(private val items: MutableList<MonthlyHistoryDomain>) :
    RecyclerView.Adapter<MonthlyHistoryAdapter.Viewholder>() {

    class Viewholder(val binding: ViewholderMonthlyHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    private lateinit var context: Context
    private var formatter: DecimalFormat? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MonthlyHistoryAdapter.Viewholder {
        context = parent.context
        formatter = DecimalFormat("###,###,###.##")
        val binding = ViewholderMonthlyHistoryBinding.inflate(LayoutInflater.from(context), parent, false)
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: MonthlyHistoryAdapter.Viewholder, position: Int) {
        val item = items[position]

        holder.binding.monthYearTxt.text = item.monthYear
        holder.binding.balanceTxt.text = "${formatter?.format(item.totalBalance)} lei"
        holder.binding.incomeTxt.text = "${formatter?.format(item.totalIncome)} lei"
        holder.binding.expenseTxt.text = "${formatter?.format(item.totalExpense)} lei"
    }

    override fun getItemCount(): Int = items.size

    fun addItem(monthlyHistory: MonthlyHistoryDomain) {
        try {
            // Check if entry for this month already exists
            val existingIndex = items.indexOfFirst { 
                it.monthYear == monthlyHistory.monthYear 
            }
            
            if (existingIndex >= 0) {
                val existingEntry = items[existingIndex]
                
                // Updates blocked for finalized months
                if (existingEntry.isFinalized) return
                
                // Update existing entry (only if not finalized)
                items[existingIndex] = monthlyHistory
                notifyItemChanged(existingIndex)
            } else {
                // Add new entry at the beginning (most recent first)
                items.add(0, monthlyHistory)
                notifyItemInserted(0)
            }
            saveMonthlyHistory()
        } catch (e: Exception) {
            println("Error adding monthly history item: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun saveMonthlyHistory() {
        try {
            val sharedPreferences = context.getSharedPreferences("MonthlyHistoryPrefs", Context.MODE_PRIVATE)
            val gson = Gson()
            val json = gson.toJson(items)
            sharedPreferences.edit().putString("monthly_history", json).apply()
        } catch (e: Exception) {
            println("Error saving monthly history: ${e.message}")
        }
    }

    companion object {
        fun loadSavedMonthlyHistory(context: Context): MutableList<MonthlyHistoryDomain> {
            return try {
                val sharedPreferences = context.getSharedPreferences("MonthlyHistoryPrefs", Context.MODE_PRIVATE)
                val gson = Gson()
                val json = sharedPreferences.getString("monthly_history", null)
                
                if (json != null) {
                    val type = object : TypeToken<MutableList<MonthlyHistoryDomain>>() {}.type
                    gson.fromJson(json, type)
                } else {
                    mutableListOf()
                }
            } catch (e: Exception) {
                println("Error loading monthly history: ${e.message}")
                mutableListOf()
            }
        }

        fun calculateCurrentMonthSummary(context: Context): MonthlyHistoryDomain {
            return try {
                val calendar = Calendar.getInstance()
                val monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)

                // Get total balance from all cards with error handling
                val totalBalance = try {
                    val savedCards = CardAdapter.loadSavedCards(context)
                    savedCards.sumOf { it.balance }
                } catch (e: Exception) {
                    println("Error calculating total balance: ${e.message}")
                    0.0
                }

                // Get all expenses for current month with error handling
                val totalExpense = try {
                    val savedExpenses = ExpenseListAdapter.loadSavedExpenses(context)
                    val currentMonthExpenses = savedExpenses.filter { expense ->
                        try {
                            val expenseDate = parseExpenseDate(expense.time)
                            if (expenseDate != null) {
                                val expenseCalendar = Calendar.getInstance()
                                expenseCalendar.time = expenseDate
                                expenseCalendar.get(Calendar.MONTH) == currentMonth &&
                                expenseCalendar.get(Calendar.YEAR) == currentYear
                            } else {
                                false
                            }
                        } catch (e: Exception) {
                            false
                        }
                    }
                    currentMonthExpenses.sumOf { it.price }
                } catch (e: Exception) {
                    println("Error calculating total expenses: ${e.message}")
                    0.0
                }

                // Calculate total income with simplified approach
                val totalIncome = calculateMonthlyIncome(context, currentMonth, currentYear)

                MonthlyHistoryDomain(
                    monthYear = monthYear,
                    totalBalance = totalBalance,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    timestamp = calendar.timeInMillis,
                    isFinalized = false // Current month is never finalized - can be updated
                )
            } catch (e: Exception) {
                println("Error calculating current month summary: ${e.message}")
                e.printStackTrace()
                // Return default values in case of error
                val calendar = Calendar.getInstance()
                val monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
                MonthlyHistoryDomain(
                    monthYear = monthYear,
                    totalBalance = 0.0,
                    totalIncome = 0.0,
                    totalExpense = 0.0,
                    timestamp = calendar.timeInMillis,
                    isFinalized = false
                )
            }
        }

        private fun parseExpenseDate(timeString: String): Date? {
            return try {
                // The expense time format is "HH:mm • dd MMM yyyy" (e.g., "14:30 • 15 Dec 2024")
                val dateFormat = SimpleDateFormat("HH:mm • dd MMM yyyy", Locale.getDefault())
                dateFormat.parse(timeString)
            } catch (e: Exception) {
                // Fallback: try parsing just the date part after the bullet
                try {
                    val parts = timeString.split(" • ")
                    if (parts.size == 2) {
                        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        dateFormat.parse(parts[1])
                    } else {
                        null
                    }
                } catch (e2: Exception) {
                    println("Error parsing expense date: $timeString, ${e2.message}")
                    null
                }
            }
        }

        private fun calculateMonthlyIncome(context: Context, month: Int, year: Int): Double {
            // Gets all cards
            // For each card: reads income from SecureTokenStorage
            // Sums all incomes together
            // Returns total with error handling
            return try {
                // Get all cards
                val savedCards = CardAdapter.loadSavedCards(context)
                var totalIncome = 0.0
                
                savedCards.forEach { card ->
                    try {
                        // Get income for each card from SecureTokenStorage
                        val secureTokenStorage = com.example.project1912.Utils.SecureTokenStorage(context)
                        val incomeString = secureTokenStorage.getIncomeForCard(card.cardNumber)
                        
                        if (incomeString != null) {
                            // Parse the income value (remove "lei" and convert to double)
                            val cleanIncomeString = incomeString.replace(" lei", "").replace("lei", "").trim()
                            val incomeValue = cleanIncomeString.toDoubleOrNull() ?: 0.0
                            totalIncome += incomeValue
                            println("Added income for card ${card.cardNumber}: $incomeValue lei")
                        } else {
                            println("No income data found for card: ${card.cardNumber}")
                        }
                    } catch (e: Exception) {
                        println("Error reading income for card ${card.cardNumber}: ${e.message}")
                    }
                }
                
                println("Total calculated monthly income: $totalIncome lei")
                totalIncome
                
            } catch (e: Exception) {
                println("Error calculating monthly income: ${e.message}")
                0.0
            }
        }

        fun generateMonthlyHistoryIfNeeded(context: Context) {
            try {
                val calendar = Calendar.getInstance()
                val currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                
                // Check if it's the first day of the month
                if (currentDayOfMonth == 1) {
                    // Calculate previous month
                    calendar.add(Calendar.MONTH, -1)
                    val previousMonthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
                    val previousMonth = calendar.get(Calendar.MONTH)
                    val previousYear = calendar.get(Calendar.YEAR)
                    
                    // Check if we already have a summary for the previous month
                    val savedHistory = loadSavedMonthlyHistory(context)
                    val existingEntry = savedHistory.find { it.monthYear == previousMonthYear }
                    
                    if (existingEntry == null) {
                        // Generate FINALIZED summary for the previous month
                        val summary = calculatePreviousMonthSummary(context, previousMonth, previousYear, previousMonthYear)
                        val finalizedSummary = summary.copy(isFinalized = true) // Mark as finalized
                        
                        // Create a temporary adapter to add the item
                        val tempAdapter = MonthlyHistoryAdapter(savedHistory)
                        tempAdapter.context = context
                        tempAdapter.addItem(finalizedSummary)
                        
                        println("Auto-generated FINALIZED monthly history for: $previousMonthYear")
                    } else if (!existingEntry.isFinalized) {
                        // If entry exists but is not finalized, finalize it now
                        val finalizedSummary = existingEntry.copy(isFinalized = true)
                        
                        val tempAdapter = MonthlyHistoryAdapter(savedHistory)
                        tempAdapter.context = context
                        tempAdapter.addItem(finalizedSummary)
                        
                        println("Finalized existing monthly history for: $previousMonthYear")
                    } else {
                        println("Monthly history already finalized for: $previousMonthYear")
                    }
                } else {
                    println("Not the first day of month (day: $currentDayOfMonth), skipping auto-generation")
                }
            } catch (e: Exception) {
                println("Error generating monthly history: ${e.message}")
                e.printStackTrace()
            }
        }

        fun calculatePreviousMonthSummary(context: Context, month: Int, year: Int, monthYear: String): MonthlyHistoryDomain {
            return try {
                // Get total balance from all cards (current balance as snapshot)
                val totalBalance = try {
                    val savedCards = CardAdapter.loadSavedCards(context)
                    savedCards.sumOf { it.balance }
                } catch (e: Exception) {
                    println("Error calculating total balance: ${e.message}")
                    0.0
                }

                // Get all expenses for the specified month
                val totalExpense = try {
                    val savedExpenses = ExpenseListAdapter.loadSavedExpenses(context)
                    val monthExpenses = savedExpenses.filter { expense ->
                        try {
                            val expenseDate = parseExpenseDate(expense.time)
                            if (expenseDate != null) {
                                val expenseCalendar = Calendar.getInstance()
                                expenseCalendar.time = expenseDate
                                expenseCalendar.get(Calendar.MONTH) == month &&
                                expenseCalendar.get(Calendar.YEAR) == year
                            } else {
                                false
                            }
                        } catch (e: Exception) {
                            false
                        }
                    }
                    monthExpenses.sumOf { it.price }
                } catch (e: Exception) {
                    println("Error calculating total expenses for $monthYear: ${e.message}")
                    0.0
                }

                // Calculate total income for the specified month
                val totalIncome = calculateMonthlyIncome(context, month, year)

                val currentTime = System.currentTimeMillis()

                MonthlyHistoryDomain(
                    monthYear = monthYear,
                    totalBalance = totalBalance,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    timestamp = currentTime,
                    isFinalized = true // Previous months are always finalized
                )
            } catch (e: Exception) {
                println("Error calculating previous month summary for $monthYear: ${e.message}")
                e.printStackTrace()
                // Return default values in case of error
                MonthlyHistoryDomain(
                    monthYear = monthYear,
                    totalBalance = 0.0,
                    totalIncome = 0.0,
                    totalExpense = 0.0,
                    timestamp = System.currentTimeMillis(),
                    isFinalized = true
                )
            }
        }

        // Testing method - simulates first day of month to generate previous month summary
        fun generatePreviousMonthSummaryForTesting(context: Context) {
            try {
                val calendar = Calendar.getInstance()
                
                // Calculate previous month
                calendar.add(Calendar.MONTH, -1)
                val previousMonthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
                val previousMonth = calendar.get(Calendar.MONTH)
                val previousYear = calendar.get(Calendar.YEAR)
                
                // Check if we already have a summary for the previous month
                val savedHistory = loadSavedMonthlyHistory(context)
                val existingEntry = savedHistory.find { it.monthYear == previousMonthYear }
                
                if (existingEntry == null) {
                    // Generate summary for the previous month
                    val summary = calculatePreviousMonthSummary(context, previousMonth, previousYear, previousMonthYear)
                    
                    // Create a temporary adapter to add the item
                    val tempAdapter = MonthlyHistoryAdapter(savedHistory)
                    tempAdapter.context = context
                    tempAdapter.addItem(summary)
                    
                    println("TEST: Generated monthly history for: $previousMonthYear")
                } else {
                    println("TEST: Monthly history already exists for: $previousMonthYear")
                }
            } catch (e: Exception) {
                println("Error in test generation: ${e.message}")
                e.printStackTrace()
            }
        }

        // Helper method to check finalization status
        fun checkFinalizationStatus(context: Context) {
            try {
                val savedHistory = loadSavedMonthlyHistory(context)
                println("=== FINALIZATION STATUS ===")
                savedHistory.forEach { entry ->
                    val status = if (entry.isFinalized) "FINALIZED" else "LIVE"
                    println("${entry.monthYear}: $status")
                }
                println("===========================")
            } catch (e: Exception) {
                println("Error checking finalization status: ${e.message}")
            }
        }
    }
} 