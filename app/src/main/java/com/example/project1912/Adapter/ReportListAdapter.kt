package com.example.personalfinanceapp.Adapter

import android.content.Context
import android.icu.text.DecimalFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.personalfinanceapp.Domain.BudgetDomain
import com.example.personalfinanceapp.Domain.ExpenseDomain
import com.example.personalfinanceapp.R
import com.example.personalfinanceapp.databinding.ViewholderBudgetBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class ReportListAdapter(private val items: MutableList<BudgetDomain>, private val context: Context) :
    RecyclerView.Adapter<ReportListAdapter.Viewholder>() {
    class Viewholder(val binding: ViewholderBudgetBinding) : RecyclerView.ViewHolder(binding.root)

    var formatter: DecimalFormat? = null
    
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReportListAdapter.Viewholder {
        formatter = DecimalFormat("###,###,###,###")
        val binding = ViewholderBudgetBinding.inflate(LayoutInflater.from(context), parent, false)
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: ReportListAdapter.Viewholder, position: Int) {
        val item = items[position]

        // Calculate actual spending percentage for this budget (current month only)
        val actualPercentage = calculateBudgetUsage(item.title)
        
        holder.binding.titleTxt.text = item.title
        holder.binding.percentTxt.text = "%.1f%%".format(actualPercentage)
        holder.binding.priceTxt.text = formatter?.format(item.price) + " lei /Month"

        holder.binding.circularProgressBar.apply {
            // Cap progress at 100% for visual display, but show actual percentage in text
            progress = minOf(actualPercentage.toFloat(), 100f)

            if (position % 2 == 1) {
                progressBarColor = context.resources.getColor(R.color.blue)
                holder.binding.percentTxt.setTextColor(context.resources.getColor(R.color.blue))
            } else {
                progressBarColor = context.resources.getColor(R.color.pink)
                holder.binding.percentTxt.setTextColor(context.resources.getColor(R.color.pink))
            }
        }
    }

    private fun calculateBudgetUsage(budgetTitle: String): Double {
        return try {
            // Load saved expenses
            val savedExpenses = loadSavedExpenses(context)
            
            // Find the budget to get its monthly limit
            val budget = items.find { it.title == budgetTitle } ?: return 0.0
            
            // Get current month and year
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)
            
            // Calculate total spending for this budget in CURRENT MONTH ONLY
            val currentMonthExpenses = savedExpenses.filter { expense ->
                try {
                    // Only include expenses that are assigned to this budget AND from current month
                    if (expense.budget == budgetTitle) {
                        val expenseDate = parseExpenseDate(expense.time)
                        if (expenseDate != null) {
                            val expenseCalendar = Calendar.getInstance()
                            expenseCalendar.time = expenseDate
                            expenseCalendar.get(Calendar.MONTH) == currentMonth &&
                            expenseCalendar.get(Calendar.YEAR) == currentYear
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
            }
            
            val totalSpent = currentMonthExpenses.sumOf { it.price }
            
            // Calculate percentage: (spent / budget) * 100
            return if (budget.price > 0) {
                (totalSpent / budget.price) * 100.0
            } else {
                0.0
            }
        } catch (e: Exception) {
            println("Error calculating budget usage for $budgetTitle: ${e.message}")
            0.0
        }
    }

    // Helper method to parse expense date in the format "HH:mm • dd MMM yyyy"
    private fun parseExpenseDate(timeString: String): Date? {
        return try {
            val format = SimpleDateFormat("HH:mm • dd MMM yyyy", Locale.getDefault())
            format.parse(timeString)
        } catch (e: Exception) {
            println("Error parsing expense date: $timeString - ${e.message}")
            null
        }
    }

    override fun getItemCount(): Int = items.size

    fun addItem(budget: BudgetDomain) {
        items.add(0, budget)  // Add to the beginning of the list
        notifyItemInserted(0)
    }

    fun removeItem(position: Int) {
        // Get the budget title before removing it
        val budgetToDelete = items[position]
        val budgetTitle = budgetToDelete.title
        
        // Clean up all transaction mappings that reference this budget
        ExpenseListAdapter.cleanupMappingsForDeletedBudget(context, budgetTitle)
        
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun saveBudgets() {
        val sharedPreferences = context.getSharedPreferences("BudgetsPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(items)
        sharedPreferences.edit().putString("budgets", json).apply()
    }

    // Method to refresh budget percentages when expenses change
    fun refreshBudgetPercentages() {
        notifyDataSetChanged()
    }

    companion object {
        fun loadSavedBudgets(context: Context): MutableList<BudgetDomain> {
            val sharedPreferences = context.getSharedPreferences("BudgetsPrefs", Context.MODE_PRIVATE)
            val gson = Gson()
            val json = sharedPreferences.getString("budgets", null)
            
            return if (json != null) {
                val type = object : TypeToken<MutableList<BudgetDomain>>() {}.type
                gson.fromJson(json, type)
            } else {
                mutableListOf()
            }
        }

        private fun loadSavedExpenses(context: Context): MutableList<ExpenseDomain> {
            val sharedPreferences = context.getSharedPreferences("ExpensesPrefs", Context.MODE_PRIVATE)
            val gson = Gson()
            val json = sharedPreferences.getString("expenses", null)
            
            return if (json != null) {
                val type = object : TypeToken<MutableList<ExpenseDomain>>() {}.type
                gson.fromJson(json, type)
            } else {
                mutableListOf()
            }
        }

        /**
         * Resets all budget percentages to 0% by ensuring only current month expenses are counted.
         * This method is automatically called when a new month begins.
         */
        fun resetMonthlyBudgetPercentages(context: Context) {
            try {
                println("=== MONTHLY BUDGET RESET ===")
                println("Resetting all budget percentages to 0% for the new month")
                
                // Save the reset timestamp for logging
                val sharedPreferences = context.getSharedPreferences("BudgetResetPrefs", Context.MODE_PRIVATE)
                val currentTime = System.currentTimeMillis()
                sharedPreferences.edit().putLong("last_budget_reset", currentTime).apply()
                
                val calendar = Calendar.getInstance()
                val monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
                println("Budget percentages reset completed for: $monthYear")
                
                // The actual reset happens automatically because calculateBudgetUsage() 
                // now only considers current month expenses
                println("All budgets will now show 0% until new expenses are added this month")
                println("============================")
                
            } catch (e: Exception) {
                println("Error resetting monthly budget percentages: ${e.message}")
                e.printStackTrace()
            }
        }

        /**
         * Checks if budget reset is needed (called on first day of month)
         */
        fun checkAndResetBudgetsIfNeeded(context: Context) {
            try {
                val calendar = Calendar.getInstance()
                val currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                
                // Check if it's the first day of the month
                if (currentDayOfMonth == 1) {
                    val sharedPreferences = context.getSharedPreferences("BudgetResetPrefs", Context.MODE_PRIVATE)
                    val lastResetTime = sharedPreferences.getLong("last_budget_reset", 0)
                    
                    // Check if we've already reset this month
                    val lastResetCalendar = Calendar.getInstance()
                    lastResetCalendar.timeInMillis = lastResetTime
                    
                    val currentMonth = calendar.get(Calendar.MONTH)
                    val currentYear = calendar.get(Calendar.YEAR)
                    val lastResetMonth = lastResetCalendar.get(Calendar.MONTH)
                    val lastResetYear = lastResetCalendar.get(Calendar.YEAR)
                    
                    // Only reset if we haven't already reset this month
                    if (lastResetTime == 0L || 
                        lastResetMonth != currentMonth || 
                        lastResetYear != currentYear) {
                        
                        resetMonthlyBudgetPercentages(context)
                    } else {
                        println("Budget reset already completed for this month")
                    }
                } else {
                    println("Not the first day of month (day: $currentDayOfMonth), skipping budget reset check")
                }
            } catch (e: Exception) {
                println("Error checking budget reset: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
