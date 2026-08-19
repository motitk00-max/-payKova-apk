package com.example.domain.model

data class ContactInfo(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val email: String? = null,
    val photoUri: String? = null
)

data class AppInfo(
    val appName: String,
    val packageName: String,
    val category: String = "App"
)
