package com.example.project1912.Activity

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project1912.Adapter.ReportListAdapter
import com.example.project1912.ViewModel.MainViewModel
import com.example.project1912.databinding.ActivityReportBinding
import com.example.project1912.Utils.SecureTokenStorage

class ReportActivity : AppCompatActivity() {
    lateinit var binding: ActivityReportBinding
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SecureTokenStorage
        val secureTokenStorage = SecureTokenStorage(this)

        // Get the card ID from the intent
        val cardId = intent.getStringExtra("CARD_ID")

        // Get the balance from the intent or saved storage for this specific card
        val balance = if (cardId != null) {
            intent.getStringExtra("BALANCE") ?: secureTokenStorage.getBankBalance(cardId)
        } else {
            null
        }

        if (balance != null) {
            binding.textView11.text = balance
        }

        // Get and display income and expense totals
        val income = intent.getStringExtra("INCOME")
        val expense = intent.getStringExtra("EXPENSE")
        
        Log.d("ReportActivity", "Received income: $income")
        Log.d("ReportActivity", "Received expense: $expense")

        // Set the values with fallback to "0 lei"
        binding.textView13.text = income ?: "0 lei"
        binding.textView16.text = expense ?: "0 lei"

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        initRecyclerview()
        setVariable()
    }

    private fun setVariable() {
        binding.backBtn.setOnClickListener { finish() }
    }

    private fun initRecyclerview() {
        binding.view2.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.view2.adapter = ReportListAdapter(mainViewModel.loadBudget())
        binding.view2.isNestedScrollingEnabled = false
    }
}