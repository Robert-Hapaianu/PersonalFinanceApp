package com.example.project1912.Activity

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project1912.Adapter.ReportListAdapter
import com.example.project1912.Domain.BudgetDomain
import com.example.project1912.ViewModel.MainViewModel
import com.example.project1912.databinding.ActivityBudgetsBinding

class BudgetsActivity : AppCompatActivity() {
    lateinit var binding: ActivityBudgetsBinding
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var budgetAdapter: ReportListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        initRecyclerview()
        setVariable()
        setupNavigation()
        setupAddBudgetFab()
    }

    private fun setVariable() {
        binding.backBtn.setOnClickListener { finish() }
    }

    private fun initRecyclerview() {
        binding.view2.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        
        // Load saved budgets or use default data if none exists
        val savedBudgets = ReportListAdapter.loadSavedBudgets(this)
        budgetAdapter = if (savedBudgets.isNotEmpty()) {
            ReportListAdapter(savedBudgets)
        } else {
            ReportListAdapter(mainViewModel.loadBudget())
        }
        
        binding.view2.adapter = budgetAdapter
        binding.view2.isNestedScrollingEnabled = false

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
                budgetAdapter.removeItem(position)
                budgetAdapter.saveBudgets() // Save changes after deletion
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

        itemTouchHelper.attachToRecyclerView(binding.view2)
    }

    private fun setupAddBudgetFab() {
        binding.addBudgetFab.setOnClickListener {
            showAddBudgetDialog()
        }
    }

    private fun showAddBudgetDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        val titleLabel = TextView(this).apply {
            text = "Budget Title"
            setTextColor(ContextCompat.getColor(this@BudgetsActivity, com.example.project1912.R.color.darkblue))
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }
        layout.addView(titleLabel)

        val titleEdit = EditText(this).apply {
            hint = "Enter budget title"
        }
        layout.addView(titleEdit)

        val spacing1 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                20
            )
        }
        layout.addView(spacing1)

        val amountLabel = TextView(this).apply {
            text = "Monthly Amount (lei)"
            setTextColor(ContextCompat.getColor(this@BudgetsActivity, com.example.project1912.R.color.darkblue))
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }
        layout.addView(amountLabel)

        val amountEdit = EditText(this).apply {
            hint = "Enter monthly amount"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or 
                       android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        layout.addView(amountEdit)

        AlertDialog.Builder(this)
            .setTitle("Add New Budget")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val title = titleEdit.text.toString()
                val amount = amountEdit.text.toString().toDoubleOrNull()

                if (title.isNotEmpty() && amount != null && amount > 0) {
                    val newBudget = BudgetDomain(
                        title = title,
                        price = amount,
                        percent = 0.0  // Always start with 0% progress
                    )
                    
                    budgetAdapter.addItem(newBudget)
                    budgetAdapter.saveBudgets()
                } else {
                    Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()

        // Show keyboard automatically
        titleEdit.requestFocus()
    }

    private fun setupNavigation() {
        // Home button navigation
        binding.homeNavButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        // Budgets button (current screen) - no action needed
        binding.budgetsNavButton.setOnClickListener {
            // Already on Budgets screen, do nothing or scroll to top
            binding.scrollView3.post {
                binding.scrollView3.scrollTo(0, 0)
            }
        }

        // History button navigation
        binding.historyNavButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Check and reset budget percentages if new month started
        com.example.project1912.Adapter.ReportListAdapter.checkAndResetBudgetsIfNeeded(this)
        
        // Refresh budget percentages when returning to this activity
        // This ensures the progress circles are updated if expenses were added/modified
        budgetAdapter.refreshBudgetPercentages()
    }
} 