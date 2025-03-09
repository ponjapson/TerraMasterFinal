package com.example.terramaster

interface ClickListener {
    fun onItemClick(userId: String, fullName: String, profilePicUrl: String)
}