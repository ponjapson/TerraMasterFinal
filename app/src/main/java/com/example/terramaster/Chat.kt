package com.example.terramaster

data class Chat(
    val chatId: String = "",
    val profilePicture: String? = null,
    val fullName: String? = null,
    val participants: List<String> = emptyList(),
    val content: String = "",
    val timestamp: Long = 0
) {
    constructor() : this("", "", "", emptyList(), "", 0)
}
