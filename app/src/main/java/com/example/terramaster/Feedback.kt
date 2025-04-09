package com.example.terramaster

data class Feedback(
    val landOwnerUserId: String = "",
    val professionalId: String = "",
    var feedback: String = "",
    var rating: Float = 0f,
    var first_name: String = "",
    var last_name: String = "",
    var profile_picture: String = ""
)
