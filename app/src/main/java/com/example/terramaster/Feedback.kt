package com.example.terramaster

data class Feedback(
    val landownerId: String = "",
    val professionalId: String = "",
    val comment: String = "",
    val ratings: Float = 0f,
    var first_name: String = "",
    var last_name: String = "",
    var profile_picture: String = ""
)
