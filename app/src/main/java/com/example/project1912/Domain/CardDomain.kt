package com.example.personalfinanceapp.Domain

import android.os.Parcel
import android.os.Parcelable

data class CardDomain(
    val cardNumber: String = "",
    val expiryDate: String = "",
    val cardType: String = "", // "visa" or "mastercard" etc.
    var balance: Double = 0.0
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(cardNumber)
        parcel.writeString(expiryDate)
        parcel.writeString(cardType)
        parcel.writeDouble(balance)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CardDomain> {
        override fun createFromParcel(parcel: Parcel): CardDomain {
            return CardDomain(parcel)
        }

        override fun newArray(size: Int): Array<CardDomain?> {
            return arrayOfNulls(size)
        }
    }
}
