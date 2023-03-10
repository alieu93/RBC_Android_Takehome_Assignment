package com.example.rbc_android_takehome_adam.data

import java.io.Serializable

data class AccountData(
    val balance: String,
    val name: String,
    val number: String,
    val type: AccountDataType
) : Serializable {

    fun isCreditAccount(): Boolean {
        return AccountDataType.CREDIT_CARD == type
    }
}