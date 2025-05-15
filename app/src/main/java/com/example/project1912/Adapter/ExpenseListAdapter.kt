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

        holder.binding.titleTxt.text = item.title
        holder.binding.timeTxt.text = item.time
        holder.binding.priceTxt.text = "$" + formatter?.format(item.price)
        val drawableResourceId =
            holder.itemView.resources.getIdentifier(item.pic, "drawable", context.packageName)

        Glide.with(context)
            .load(drawableResourceId)
            .into(holder.binding.pic)

        holder.itemView.setOnClickListener {
            showEditDialog(position)
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

        val spacing = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                20
            )
        }
        layout.addView(spacing)

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

        AlertDialog.Builder(context)
            .setTitle("Edit Expense")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = activityEdit.text.toString()
                val newPrice = priceEdit.text.toString().toDoubleOrNull() ?: item.price

                items[position] = item.copy(
                    title = newTitle,
                    price = newPrice
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
    }

    fun removeItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun addItem(expense: ExpenseDomain) {
        items.add(0, expense)  // Add to the beginning of the list
        notifyItemInserted(0)
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