package com.example.project1912.Activity

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.project1912.databinding.ActivityReportBinding
import com.example.project1912.Utils.SecureTokenStorage

class ReportActivity : AppCompatActivity() {
    lateinit var binding: ActivityReportBinding

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

        setVariable()
    }

    private fun setVariable() {
        binding.backBtn.setOnClickListener { finish() }
    }
}