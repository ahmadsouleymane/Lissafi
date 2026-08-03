package com.lissafi.app

import android.app.Application

class LissafiApp : Application() {
    val database by lazy { LissafiDatabase.getInstance(this) }
}
