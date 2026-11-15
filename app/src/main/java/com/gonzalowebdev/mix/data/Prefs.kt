package com.gonzalowebdev.mix.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("mix", Context.MODE_PRIVATE)

    var imageSeconds: Int
        get() = sp.getInt("imageSeconds", 5)
        set(v) = sp.edit().putInt("imageSeconds", v).apply()

    var shuffle: Boolean
        get() = sp.getBoolean("shuffle", false)
        set(v) = sp.edit().putBoolean("shuffle", v).apply()

    /** 0 = no repetir, 1 = repetir todo, 2 = repetir uno */
    var repeatMode: Int
        get() = sp.getInt("repeatMode", 1)
        set(v) = sp.edit().putInt("repeatMode", v).apply()
}
