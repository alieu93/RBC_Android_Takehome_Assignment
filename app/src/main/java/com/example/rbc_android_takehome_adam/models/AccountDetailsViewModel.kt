package com.example.rbc_android_takehome_adam.models

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rbc.rbcaccountlibrary.AccountProvider
import com.rbc.rbcaccountlibrary.Transaction
import kotlinx.coroutines.*

class AccountDetailsViewModel(private val accountProvider: AccountProvider) : ViewModel() {
    val hasTransactionError = MutableLiveData<Boolean>()
    val showTransactionError = MutableLiveData<Boolean>()
    val isLoading = MutableLiveData<Boolean>()
    private lateinit var _currentAccountData: AccountData
    var currentAccountData: AccountData
        get() = _currentAccountData
        set(value) {
            _currentAccountData = value
        }

    private var _allTransactionsList: List<Transaction> = emptyList()
    val allTransactionList = MutableLiveData<List<Transaction>>()

    private val exceptionHandler = CoroutineExceptionHandler { _, t ->
        t.printStackTrace()
        onError("Exception: ${t.localizedMessage}")
    }

    fun getTransactionsForAccount() {
        val allTransactions = mutableListOf<Transaction>()
        CoroutineScope(Dispatchers.IO + exceptionHandler).launch {
            if (_currentAccountData.isCreditAccount()) {
                val creditTransactionDeferred = async {
                    getCreditTransactions(_currentAccountData.number)
                }
                allTransactions.addAll(creditTransactionDeferred.await())
            }

            val transactionDeferred = async {
                getTransactions(_currentAccountData.number)
            }
            allTransactions.addAll(transactionDeferred.await())

            //Possible for one call to fail but not the other, so if both calls (or just one for all non-credit card), show error
            if (hasTransactionError.value == true && allTransactions.isEmpty()) {
                isLoading.value = false
                showTransactionError.value = true
            } else {
                addToAllTransactionList(allTransactions)
            }
        }
    }

    private fun addToAllTransactionList(transactions: MutableList<Transaction>) {
        //Because the assignment requirements are on newest first only, I chose to keep this here
        //if it were ever expanded upon to including sorting by earliest to newest, this would not be good design
        transactions.sortByDescending { transaction ->
            transaction.date
        }
        _allTransactionsList = transactions
        isLoading.value = false
        allTransactionList.postValue(_allTransactionsList)
    }

    //Observations were made that both these transaction call will fail from time to time
    //As such, I included a catch statement to return emptyList and a check later to show no transactions found or a "silent" failure
    //In regular circumstances, this would
    private fun getTransactions(accountNumber: String): List<Transaction> {
        try {
            return accountProvider.getTransactions(accountNumber)
        } catch (e: Exception) {
            hasTransactionError.value = true
            e.printStackTrace()
        }
        return emptyList()
    }

    private fun getCreditTransactions(accountNumber: String): List<Transaction> {
        try {
            return accountProvider.getAdditionalCreditCardTransactions(accountNumber)
        } catch (e: Exception) {
            hasTransactionError.value = true
            e.printStackTrace()
        }
        return emptyList()
    }

    private fun onError(message: String) {
        Log.e("Error", message)
    }
}