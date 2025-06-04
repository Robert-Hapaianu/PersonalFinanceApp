package com.example.project1912.Adapter

import android.app.AlertDialog
import android.content.Context
import android.icu.text.DecimalFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.project1912.Domain.ExpenseDomain
import com.example.project1912.databinding.ViewholderItemsBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ExpenseListAdapter(private val items: MutableList<ExpenseDomain>) :
    RecyclerView.Adapter<ExpenseListAdapter.Viewholder>() {

    class Viewholder(val binding: ViewholderItemsBinding) : RecyclerView.ViewHolder(binding.root)

    private lateinit var context: Context
    var formatter: DecimalFormat? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ExpenseListAdapter.Viewholder {
        context = parent.context
        formatter = DecimalFormat("###,###,###.##")
        val binding = ViewholderItemsBinding.inflate(LayoutInflater.from(context), parent, false)
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseListAdapter.Viewholder, position: Int) {
        val item = items[position]

        // Truncate title if longer than 16 characters for display
        val displayTitle = if (item.title.length > 16) {
            item.title.take(13) + "..."
        } else {
            item.title
        }

        holder.binding.titleTxt.text = displayTitle
        holder.binding.timeTxt.text = item.time
        holder.binding.priceTxt.text = formatter?.format(item.price) + " lei"
        val drawableResourceId =
            holder.itemView.resources.getIdentifier(item.pic, "drawable", context.packageName)

        Glide.with(context)
            .load(drawableResourceId)
            .into(holder.binding.pic)

        holder.itemView.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                showEditDialog(currentPosition)
            }
        }
    }

    private fun showEditDialog(position: Int) {
        val item = items[position]
        
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        val activityLabel = TextView(context).apply {
            text = "Activity"
            setTextColor(ContextCompat.getColor(context, com.example.project1912.R.color.darkblue))
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }
        layout.addView(activityLabel)

        val activityEdit = EditText(context).apply {
            setText(item.title)
            hint = "Enter activity name"
        }
        layout.addView(activityEdit)

        val spacing1 = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                20
            )
        }
        layout.addView(spacing1)

        val priceLabel = TextView(context).apply {
            text = "Price"
            setTextColor(ContextCompat.getColor(context, com.example.project1912.R.color.darkblue))
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }
        layout.addView(priceLabel)

        val priceEdit = EditText(context).apply {
            setText(item.price.toString())
            hint = "Enter price"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or 
                       android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        layout.addView(priceEdit)

        val spacing2 = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                20
            )
        }
        layout.addView(spacing2)

        val budgetLabel = TextView(context).apply {
            text = "Budget"
            setTextColor(ContextCompat.getColor(context, com.example.project1912.R.color.darkblue))
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }
        layout.addView(budgetLabel)

        // Load saved budgets
        val savedBudgets = com.example.project1912.Adapter.ReportListAdapter.loadSavedBudgets(context)
        val budgetTitles = mutableListOf("No Budget")
        budgetTitles.addAll(savedBudgets.map { it.title })

        val budgetSpinner = android.widget.Spinner(context).apply {
            val adapter = android.widget.ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                budgetTitles
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            this.adapter = adapter
            
            // Set current selection based on item's budget
            val currentBudgetIndex = if (item.budget != null) {
                budgetTitles.indexOf(item.budget)
            } else {
                0 // "No Budget"
            }
            setSelection(if (currentBudgetIndex >= 0) currentBudgetIndex else 0)
        }
        layout.addView(budgetSpinner)

        AlertDialog.Builder(context)
            .setTitle("Edit Expense")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = activityEdit.text.toString()
                val newPrice = priceEdit.text.toString().toDoubleOrNull() ?: item.price
                val selectedBudget = if (budgetSpinner.selectedItemPosition == 0) null else budgetTitles[budgetSpinner.selectedItemPosition]

                items[position] = item.copy(
                    title = newTitle,
                    price = newPrice,
                    budget = selectedBudget
                )
                notifyItemChanged(position)
                saveExpenses()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun saveExpenses() {
        val sharedPreferences = context.getSharedPreferences("ExpensesPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(items)
        sharedPreferences.edit().putString("expenses", json).apply()
        
        // Notify that budgets may need to be updated
        notifyBudgetUpdate()
    }

    private fun notifyBudgetUpdate() {
        // Send a broadcast or use other mechanism to notify budget changes
        // For simplicity, we'll use a static flag that BudgetsActivity can check
        val prefs = context.getSharedPreferences("BudgetUpdatePrefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_expense_update", System.currentTimeMillis()).apply()
    }

    fun removeItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun addItem(expense: ExpenseDomain) {
        items.add(0, expense)  // Add to the beginning of the list
        notifyItemInserted(0)
        saveExpenses() // Save immediately after adding the item
    }

    fun removeTransactionsByCardId(cardId: String) {
        val iterator = items.iterator()
        var index = 0
        val itemsToRemove = mutableListOf<Int>()
        
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.cardId == cardId) {
                itemsToRemove.add(index)
            }
            index++
        }
        
        // Remove items in reverse order to maintain correct indices
        for (i in itemsToRemove.reversed()) {
            items.removeAt(i)
            notifyItemRemoved(i)
        }
        
        saveExpenses() // Save changes after deletion
    }

    override fun getItemCount(): Int = items.size

    companion object {
        fun loadSavedExpenses(context: Context): MutableList<ExpenseDomain> {
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