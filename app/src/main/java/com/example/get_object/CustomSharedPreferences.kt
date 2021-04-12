package com.example.get_object

import android.content.Context

public class CustomSharedPreferences(context: Context) {

    companion object{
        const val DATA_KEY = "Data"
    }
    private val data = context.getSharedPreferences(DATA_KEY, Context.MODE_PRIVATE)

    public fun setBooleanShared(key: String, value: Boolean){
        val editor = data.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    public fun getBooleanShared(key: String): Boolean {
        return data.getBoolean(key, false)
    }

}