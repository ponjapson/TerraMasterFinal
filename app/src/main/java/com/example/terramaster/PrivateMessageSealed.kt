package com.example.terramaster

sealed class PrivateMessageSealed {
    data class SentMessage(val message: Message) : PrivateMessageSealed()
    data class ReceivedMessage(val message: Message) : PrivateMessageSealed()
}