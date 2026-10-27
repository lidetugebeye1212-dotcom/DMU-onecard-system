
package com.example.dmuonecard

data class MealStatusResponse(
    val status: String = "CLOSED",
    val message: String = "",
    val meal: String? = null,
    val meal_display: String? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val served_count: Int = 0,
    val todays_total: Int = 0,
    val next_meal: String? = null,
    val next_opening: String? = null,
    val server_time: String? = null,
    val timezone: String? = null
)
