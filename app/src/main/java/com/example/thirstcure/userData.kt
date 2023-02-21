package com.example.thirstcure

import java.util.*

class userData {

    private var drink: String? = null
    private var drinkValue: String? = null
    private var dateTimestamp: Date? = null

   fun getDateTimestamp(): Date? {
       return dateTimestamp
   }

    fun setDateTimestamp(dateTimestamp: Date?) {
        this.dateTimestamp = dateTimestamp
    }

    fun getDrink(): String? {
        return drink
    }

    fun setDrink(drink: String?) {
        this.drink = drink
    }

    fun getDrinkValue(): String? {
        return drinkValue
    }

    fun setValue(drinkValue: String) {
        this.drinkValue = drinkValue
    }


    override fun toString(): String {
        return "DrinkData{" +
                ", drink=" + drink +
                ", drinkValue='" + drinkValue +
                " , dateTimestamp=" + dateTimestamp +
                '}'

    }
}