package com.example.project1912.Adapter

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.project1912.Activity.ReportActivity
import com.example.project1912.Domain.CardDomain
import com.example.project1912.R
import com.example.project1912.databinding.ViewholderCardBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CardAdapter(private val cards: MutableList<CardDomain>, private val context: Context) :
    RecyclerView.Adapter<CardAdapter.Viewholder>() {

    private val PREFS_NAME = "CardPrefs"
    private val CARDS_KEY = "saved_cards"
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        loadCards()
    }

    class Viewholder(val binding: ViewholderCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
        val binding = ViewholderCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        val card = cards[position]
        holder.binding.apply {
            textView4.text = card.cardNumber
            textView5.text = card.expiryDate
            
            // Set card type icon
            val cardTypeIcon = when (card.cardType.lowercase()) {
                "brd" -> R.drawable.sim
                else -> R.drawable.visa
            }
            imageView3.setImageResource(cardTypeIcon)
            imageView4.setImageResource(cardTypeIcon)

            // Add click listener for card
            root.setOnClickListener {
                val context = root.context
                context.startActivity(Intent(context, ReportActivity::class.java))
            }

            // Add click listener for delete button
            deleteButton.setOnClickListener {
                val position = holder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    cards.removeAt(position)
                    notifyItemRemoved(position)
                    saveCards() // Save after deletion
                }
            }
        }
    }

    override fun getItemCount(): Int = cards.size

    fun addCard(card: CardDomain) {
        cards.add(card)
        notifyItemInserted(cards.size - 1)
        saveCards() // Save after adding
    }

    private fun saveCards() {
        val json = gson.toJson(cards)
        sharedPreferences.edit().putString(CARDS_KEY, json).apply()
    }

    private fun loadCards() {
        val json = sharedPreferences.getString(CARDS_KEY, null)
        if (json != null) {
            val type = object : TypeToken<MutableList<CardDomain>>() {}.type
            val loadedCards = gson.fromJson<MutableList<CardDomain>>(json, type)
            cards.clear()
            cards.addAll(loadedCards)
            notifyDataSetChanged()
        }
    }

    companion object {
        fun loadSavedCards(context: Context): MutableList<CardDomain> {
            val prefs = context.getSharedPreferences("CardPrefs", Context.MODE_PRIVATE)
            val json = prefs.getString("saved_cards", null)
            return if (json != null) {
                val gson = Gson()
                val type = object : TypeToken<MutableList<CardDomain>>() {}.type
                gson.fromJson(json, type)
            } else {
                mutableListOf(
                    CardDomain(
                        cardNumber = "1234 5678 9012 3456",
                        expiryDate = "03/07",
                        cardType = "visa",
                        balance = 23451.58
                    )
                )
            }
        }
    }
} 