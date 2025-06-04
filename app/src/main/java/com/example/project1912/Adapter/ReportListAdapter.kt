package com.example.project1912.Adapter

import android.content.Context
import android.icu.text.DecimalFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.project1912.Domain.BudgetDomain
import com.example.project1912.Domain.ExpenseDomain
import com.example.project1912.R
import com.example.project1912.databinding.ViewholderBudgetBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ReportListAdapter(private val items: MutableList<BudgetDomain>) :
    RecyclerView.Adapter<ReportListAdapter.Viewholder>() {
    class Viewholder(val binding: ViewholderBudgetBinding) : RecyclerView.ViewHolder(binding.root)

    private lateinit var context: Context
    var formatter: DecimalFormat? = null
    
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReportListAdapter.Viewholder {
        context = parent.context
        formatter = DecimalFormat("###,###,###,###")
        val binding = ViewholderBudgetBinding.inflate(LayoutInflater.from(context), parent, false)
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: ReportListAdapter.Viewholder, position: Int) {
        val item = items[position]

        // Calculate actual spending percentage for this budget
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
        // Load saved expenses
        val savedExpenses = loadSavedExpenses(context)
        
        // Find the budget to get its monthly limit
        val budget = items.find { it.title == budgetTitle } ?: return 0.0
        
        // Calculate total spending for this budget
        val totalSpent = savedExpenses
            .filter { it.budget == budgetTitle }
            .sumOf { it.price }
        
        // Calculate percentage: (spent / budget) * 100
        return if (budget.price > 0) {
            (totalSpent / budget.price) * 100.0
        } else {
            0.0
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
    }
}