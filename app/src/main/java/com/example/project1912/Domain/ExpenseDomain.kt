package com.example.personalfinanceapp.Domain

import android.os.Parcel
import android.os.Parcelable

data class ExpenseDomain(
    val title: String = "",
    val price: Double = 0.0,
    val pic: String = "",
    val time: String = "",
     val cardId: String? = null,
    val budget: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readDouble(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeDouble(price)
        parcel.writeString(pic)
        parcel.writeString(time)
        parcel.writeString(cardId)
        parcel.writeString(budget)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ExpenseDomain> {
        override fun createFromParcel(parcel: Parcel): ExpenseDomain {
            return ExpenseDomain(parcel)
        }

        override fun newArray(size: Int): Array<ExpenseDomain?> {
            return arrayOfNulls(size)
        }
    }

}
