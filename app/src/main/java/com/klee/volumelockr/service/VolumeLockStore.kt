package com.klee.volumelockr.service

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class VolumeLockStore(private val mContext: Context) {

    companion object {
        private const val TAG = "VolumeLockStore"
        const val APP_SHARED_PREFERENCES = "volumelockr_shared_preferences"
        const val LOCKS_KEY = "locks_key"
    }

    fun load(): HashMap<Int, Int> {
        class Token : TypeToken<HashMap<Int, Int>>()

        val value = preferences().getString(LOCKS_KEY, "")
        if (value.isNullOrBlank()) {
            return HashMap()
        }

        return runCatching {
            Gson().fromJson<HashMap<Int, Int>>(value, Token().type)
        }.onFailure {
            Log.w(TAG, "Dropping corrupted volume locks", it)
        }.getOrNull() ?: HashMap()
    }

    fun save(locks: Map<Int, Int>) {
        preferences().edit {
            putString(LOCKS_KEY, Gson().toJson(locks))
        }
    }

    fun hasLocks(): Boolean = load().isNotEmpty()

    private fun preferences() =
        mContext.getSharedPreferences(APP_SHARED_PREFERENCES, Context.MODE_PRIVATE)
}
