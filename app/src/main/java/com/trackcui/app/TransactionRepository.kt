package com.trackcui.app

import androidx.lifecycle.LiveData

class TransactionRepository(private val dao: TransactionDao) {

    val allTransactions: LiveData<List<Transaction>> = dao.getAllTransactions()
    val balance: LiveData<Double?> = dao.getBalance()

    suspend fun insert(transaction: Transaction) {
        dao.insert(transaction)
    }

    suspend fun update(transaction: Transaction) {
        dao.update(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        dao.delete(transaction)
    }
}