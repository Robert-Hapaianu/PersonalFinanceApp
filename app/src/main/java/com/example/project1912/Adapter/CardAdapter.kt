package com.example.personalfinanceapp.Adapter

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.personalfinanceapp.Activity.MainActivity
import com.example.personalfinanceapp.Activity.ReportActivity
import com.example.personalfinanceapp.Domain.CardDomain
import com.example.personalfinanceapp.R
import com.example.personalfinanceapp.databinding.ViewholderCardBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CardAdapter(val cards: MutableList<CardDomain>, private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val PREFS_NAME = "CardPrefs"
    private val CARDS_KEY = "saved_cards"
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val TYPE_CARD = 0
        private const val TYPE_ADD_BUTTON = 1

        fun loadSavedCards(context: Context): MutableList<CardDomain> {
            val prefs = context.getSharedPreferences("CardPrefs", Context.MODE_PRIVATE)
            val json = prefs.getString("saved_cards", null)
            return if (json != null) {
                val gson = Gson()
                val type = object : TypeToken<MutableList<CardDomain>>() {}.type
                gson.fromJson(json, type)
            } else {
                mutableListOf<CardDomain>()
            }
        }
    }

    init {
        loadCards()
    }

    class CardViewHolder(val binding: ViewholderCardBinding) : RecyclerView.ViewHolder(binding.root)
    class AddCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_CARD -> {
                val binding = ViewholderCardBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                // Set fixed width for card items
                binding.root.layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    width = 300.dpToPx(parent.context)
                    height = 230.dpToPx(parent.context)
                    setMargins(8.dpToPx(parent.context), 16.dpToPx(parent.context), 8.dpToPx(parent.context), 16.dpToPx(parent.context))
                }
                CardViewHolder(binding)
            }
            TYPE_ADD_BUTTON -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_add_card_button, parent, false)
                // Set fixed width for add button
                view.layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    width = 300.dpToPx(parent.context)
                    height = 230.dpToPx(parent.context)
                    setMargins(8.dpToPx(parent.context), 16.dpToPx(parent.context), 8.dpToPx(parent.context), 16.dpToPx(parent.context))
                }
                AddCardViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CardViewHolder -> {
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
                        val intent = Intent(context, ReportActivity::class.java).apply {
                            putExtra("CARD_ID", card.cardNumber)
                        }
                        context.startActivity(intent)
                    }

                    // Add click listener for delete button
                    deleteButton.setOnClickListener {
                        val position = holder.adapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            val cardToDelete = cards[position]
                            
                            // Remove associated transactions from MainActivity's expense adapter
                            (context as? MainActivity)?.removeTransactionsForCard(cardToDelete.cardNumber)
                            
                            cards.removeAt(position)
                            notifyItemRemoved(position)
                            saveCards()
                        }
                    }
                }
            }
            is AddCardViewHolder -> {
                // Make sure the add card button is visible and clickable
                holder.itemView.visibility = View.VISIBLE
                holder.itemView.setOnClickListener {
                    (context as? MainActivity)?.showAddCardDialog()
                }
            }
        }
    }

    override fun getItemCount(): Int = cards.size + 1 // +1 for Add Card button

    override fun getItemViewType(position: Int): Int {
        return if (position == cards.size) TYPE_ADD_BUTTON else TYPE_CARD
    }

    fun addCard(card: CardDomain) {
        cards.add(card)
        notifyItemInserted(cards.size - 1)
        saveCards()
    }

    fun updateCardBalance(cardId: String, newBalance: Double) {
        val cardIndex = cards.indexOfFirst { it.cardNumber == cardId }
        if (cardIndex != -1) {
            cards[cardIndex].balance = newBalance
            notifyItemChanged(cardIndex)
            saveCards()
        }
    }

    fun getTotalBalance(): Double {
        return cards.sumOf { it.balance }
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

    // Extension function to convert dp to pixels
    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
} 
