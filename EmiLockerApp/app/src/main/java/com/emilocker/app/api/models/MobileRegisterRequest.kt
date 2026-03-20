package com.emilocker.app.api.models

data class MobileRegisterRequest(
    val appId: String,
    val customerName: String,
    val dob: String,
    val aadhaarNumber: String,
    val panNumber: String,
    val fatherName: String,
    val village: String,
    val clientName: String
)
