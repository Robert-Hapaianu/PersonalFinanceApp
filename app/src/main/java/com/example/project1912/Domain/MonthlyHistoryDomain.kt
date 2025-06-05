package com.example.project1912.Domain

import android.os.Parcel
import android.os.Parcelable

data class MonthlyHistoryDomain(
    val monthYear: String = "", // e.g., "December 2024"
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val timestamp: Long = 0L // For sorting and identifying entries
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(monthYear)
        parcel.writeDouble(totalBalance)
        parcel.writeDouble(totalIncome)
        parcel.writeDouble(totalExpense)
        parcel.writeLong(timestamp)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MonthlyHistoryDomain> {
        override fun createFromParcel(parcel: Parcel): MonthlyHistoryDomain {
            return MonthlyHistoryDomain(parcel)
        }

        override fun newArray(size: Int): Array<MonthlyHistoryDomain?> {
            return arrayOfNulls(size)
        }
    }
} 