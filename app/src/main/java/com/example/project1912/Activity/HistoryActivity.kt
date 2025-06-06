package com.example.project1912.Activity

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project1912.Adapter.MonthlyHistoryAdapter
import com.example.project1912.ViewModel.MainViewModel
import com.example.project1912.databinding.ActivityHistoryBinding

class HistoryActivity : AppCompatActivity() {
    lateinit var binding: ActivityHistoryBinding
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var monthlyHistoryAdapter: MonthlyHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityHistoryBinding.inflate(layoutInflater)
            setContentView(binding.root)

            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )

            initRecyclerview()
            setVariable()
            setupNavigation()
            
            // Generate monthly history if needed (check if it's the first day of the month)
            MonthlyHistoryAdapter.generateMonthlyHistoryIfNeeded(this)
            
            // Check and reset budget percentages if new month started
            com.example.project1912.Adapter.ReportListAdapter.checkAndResetBudgetsIfNeeded(this)
            
            // Check and log finalization status
            MonthlyHistoryAdapter.checkFinalizationStatus(this)
            
        } catch (e: Exception) {
            println("Error in HistoryActivity onCreate: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error loading history page", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setVariable() {
        try {
            binding.backBtn.setOnClickListener { finish() }
        } catch (e: Exception) {
            println("Error setting up back button: ${e.message}")
        }
    }

    private fun initRecyclerview() {
        try {
            binding.view2.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            
            // Load saved monthly history
            val savedMonthlyHistory = MonthlyHistoryAdapter.loadSavedMonthlyHistory(this)
            
            // If no history exists, create a current month summary for demonstration
            if (savedMonthlyHistory.isEmpty()) {
                try {
                    val currentMonthSummary = MonthlyHistoryAdapter.calculateCurrentMonthSummary(this)
                    savedMonthlyHistory.add(currentMonthSummary)
                } catch (e: Exception) {
                    println("Error creating current month summary: ${e.message}")
                    // Continue without the summary
                }
            }
            
            monthlyHistoryAdapter = MonthlyHistoryAdapter(savedMonthlyHistory)
            binding.view2.adapter = monthlyHistoryAdapter
            binding.view2.isNestedScrollingEnabled = false
            
        } catch (e: Exception) {
            println("Error initializing RecyclerView: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error loading history data", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            // Refresh the current month summary when returning to this activity
            refreshCurrentMonthSummary()
        } catch (e: Exception) {
            println("Error in onResume: ${e.message}")
        }
    }

    private fun refreshCurrentMonthSummary() {
        try {
            if (::monthlyHistoryAdapter.isInitialized) {
                // Calculate current month summary with fresh data (including updated income)
                val currentMonthSummary = MonthlyHistoryAdapter.calculateCurrentMonthSummary(this)
                monthlyHistoryAdapter.addItem(currentMonthSummary)
                
                // No longer update previous months - they should remain finalized once created
                println("Refreshed current month summary only - previous months remain finalized")
            }
        } catch (e: Exception) {
            println("Error refreshing current month summary: ${e.message}")
        }
    }

    private fun setupNavigation() {
        try {
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
                try {
                    binding.scrollView3.post {
                        binding.scrollView3.scrollTo(0, 0)
                    }
                } catch (e: Exception) {
                    println("Error scrolling to top: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("Error setting up navigation: ${e.message}")
        }
    }
} 