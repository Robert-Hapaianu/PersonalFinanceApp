package com.example.project1912.Activity

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project1912.Adapter.ExpenseListAdapter
import com.example.project1912.ViewModel.MainViewModel
import com.example.project1912.databinding.ActivityHistoryBinding

class HistoryActivity : AppCompatActivity() {
    lateinit var binding: ActivityHistoryBinding
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var expenseAdapter: ExpenseListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        initRecyclerview()
        setVariable()
        setupNavigation()
    }

    private fun setVariable() {
        binding.backBtn.setOnClickListener { finish() }
    }

    private fun initRecyclerview() {
        binding.view2.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        
        // Load saved expenses to show transaction history
        val savedExpenses = ExpenseListAdapter.loadSavedExpenses(this)
        expenseAdapter = if (savedExpenses.isNotEmpty()) {
            ExpenseListAdapter(savedExpenses)
        } else {
            ExpenseListAdapter(mainViewModel.loadData())
        }
        
        binding.view2.adapter = expenseAdapter
        binding.view2.isNestedScrollingEnabled = false
    }

    private fun setupNavigation() {
        // Home button navigation
        binding.homeNavButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        // Budgets button navigation
        binding.budgetsNavButton.setOnClickListener {
            val intent = Intent(this, BudgetsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        // History button (current screen) - no action needed
        binding.historyNavButton.setOnClickListener {
            // Already on History screen, do nothing or scroll to top
            binding.scrollView3.post {
                binding.scrollView3.scrollTo(0, 0)
            }
        }
    }
} 