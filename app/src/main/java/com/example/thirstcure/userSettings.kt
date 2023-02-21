package com.example.thirstcure

class userSettings {

    private var limit: String? = null


    fun getDaylimit(): String? {
        return limit
    }

    fun setDaylimit(limit: String?) {
        this.limit = limit
    }


    override fun toString(): String {
        return this.limit!!
    }
}