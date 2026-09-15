package com.or.daftarcha.data.model

data class WorkerProfileData(
    val employee: Employee,
    val totalWorkdays: Int = 0,
    val totalEarned: Double = 0.0,
    val totalPaid: Double = 0.0,
    val balance: Double = 0.0,
    val totalExpensesOnProjects: Double = 0.0,
    val personalExpenses: List<Expense> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val projectBreakdowns: List<WorkerProjectStat> = emptyList()
)

data class WorkerProjectStat(
    val projectId: Int,
    val projectName: String,
    val workdays: Int,
    val totalProjectExpense: Double,
    val earnedInProject: Double
)
