package com.example.personalfinanceapp.Utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureTokenStorage(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAccessToken(token: String) {
        sharedPreferences.edit().putString("access_token", token).apply()
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString("access_token", null)
    }

    fun saveRefreshToken(token: String) {
        sharedPreferences.edit().putString("refresh_token", token).apply()
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString("refresh_token", null)
    }

    fun saveRequisitionId(id: String) {
        sharedPreferences.edit().putString("requisition_id", id).apply()
    }

    fun getRequisitionId(): String? {
        return sharedPreferences.getString("requisition_id", null)
    }

    fun saveAccountId(accountId: String) {
        sharedPreferences.edit().putString("account_id", accountId).apply()
    }

    fun getAccountId(): String? {
        return sharedPreferences.getString("account_id", null)
    }

    // Card-specific account ID mapping
    fun saveAccountIdForCard(cardId: String, accountId: String) {
        sharedPreferences.edit().putString("account_id_$cardId", accountId).apply()
    }

    fun getAccountIdForCard(cardId: String): String? {
        return sharedPreferences.getString("account_id_$cardId", null)
    }

    fun saveBankBalance(cardId: String, balance: String) {
        sharedPreferences.edit().putString("bank_balance_$cardId", balance).apply()
    }

    fun getBankBalance(cardId: String): String? {
        return sharedPreferences.getString("bank_balance_$cardId", null)
    }

    fun saveIncomeForCard(cardId: String, income: String) {
        sharedPreferences.edit().putString("income_$cardId", income).apply()
    }

    fun getIncomeForCard(cardId: String): String? {
        return sharedPreferences.getString("income_$cardId", null)
    }

    fun saveExpenseForCard(cardId: String, expense: String) {
        sharedPreferences.edit().putString("expense_$cardId", expense).apply()
    }

    fun getExpenseForCard(cardId: String): String? {
        return sharedPreferences.getString("expense_$cardId", null)
    }
} 
