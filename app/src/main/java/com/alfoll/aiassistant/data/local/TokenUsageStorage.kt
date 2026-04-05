package com.alfoll.aiassistant.data.local

import android.content.Context

class TokenUsageStorage(context: Context) {

    private val preferences = context.getSharedPreferences(
        "token_usage_storage",
        Context.MODE_PRIVATE
    )

    fun getTotalTokens(): Int {
        return preferences.getInt(KEY_TOTAL_TOKENS, 0)
    }

    fun addTokens(tokens: Int) {
        if (tokens <= 0) return

        val updatedTotal = getTotalTokens() + tokens
        preferences.edit()
            .putInt(KEY_TOTAL_TOKENS, updatedTotal)
            .apply()
    }

    companion object {
        private const val KEY_TOTAL_TOKENS = "key_total_tokens"
    }
}