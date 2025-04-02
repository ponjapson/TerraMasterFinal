package com.example.terramaster

interface OnPaymentClickListener {
    fun onPayNowClicked(bookingId: String)
    fun onPayRemaining(bookingId: String)
}