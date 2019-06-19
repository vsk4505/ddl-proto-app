package com.doordash.doordashlite.repository.inDb

import android.content.Context

class CacheManager(val context: Context) {

    val NAME = "CacheManager"
    val IDS_KEY = "Key"

    val prefs by lazy {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    private var idsMutableSetSnapshot: MutableSet<Int>? = null

    val idsMutableSet: MutableSet<Int>?
        get() {
            if (idsMutableSetSnapshot == null) {
                refreshIdsSnapshot()
            }
            return idsMutableSetSnapshot
        }

    fun toggleId(id: Int) {
        prefs.getStringSet(IDS_KEY, mutableSetOf())?.let { set ->
            val idStr = id.toString()
            if (set.contains(idStr)) {
                set.remove(idStr)
            } else {
                set.add(idStr)
            }
            prefs.edit().let {
                // Just know that it is not allowed to store there is a known issue here.
                // https://stackoverflow.com/questions/21396358/sharedpreferences-putstringset-doesnt-work
                // https://developer.android.com/reference/android/content/SharedPreferences.html#getStringSet(java.lang.String,%2520java.util.Set%3Cjava.lang.String%3E)
                it.clear()
                it.putStringSet(IDS_KEY, set)
                it.commit()
            }
        }
        refreshIdsSnapshot()
    }

    private fun refreshIdsSnapshot() {
        val idsMutableSet = mutableSetOf<Int>()
        idsMutableSet.apply {
            prefs.getStringSet(IDS_KEY, mutableSetOf()).forEach {
                add(Integer.parseInt(it))
            }
        }
        idsMutableSetSnapshot = idsMutableSet
    }
}