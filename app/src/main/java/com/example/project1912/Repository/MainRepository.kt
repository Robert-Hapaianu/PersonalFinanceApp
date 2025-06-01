package com.example.project1912.Repository

import com.example.project1912.Domain.BudgetDomain
import com.example.project1912.Domain.ExpenseDomain

class MainRepository {
    val items= mutableListOf(
        ExpenseDomain("Resturant",573.12,"btn_1","17 jun 2024 19:15", null, null),
        ExpenseDomain("McDonald",77.82,"btn_1","16 jun 2024 13:57", null, null),
        ExpenseDomain("Cinema",23.47,"btn_1","16 jun 2024 20:45", null, null),
        ExpenseDomain("Resturant",341.12,"btn_1","15 jun 2024 22:18", null, null)
    )

    val budget= mutableListOf(
        BudgetDomain("Home Loan",1200.0,80.8),
        BudgetDomain("Subscription",1200.0,10.0),
        BudgetDomain("Car Loan",800.0,30.0)
    )
}