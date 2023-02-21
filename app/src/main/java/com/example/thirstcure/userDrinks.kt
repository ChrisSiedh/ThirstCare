package com.example.thirstcure

class userDrinks {

    private var drink: String? = null


    fun getDrink(): String? {
        return drink
    }

    fun setDrink(drink: String?) {
        this.drink = drink
    }


    override fun toString(): String {
        return this.drink!!
    }
}