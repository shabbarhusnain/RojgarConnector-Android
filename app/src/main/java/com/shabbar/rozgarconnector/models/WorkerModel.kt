package com.shabbar.rozgarconnector.models


data class WorkerModel(
    val uid: String = "",
    val fullName: String? = null,
    val district: String? = null,
    val dpImage: String? = null,
    val skills: String? = null, // For uneducated
    val experience: String? = null, // For uneducated
    val dailyRate: String? = null, // For uneducated
    val degreeName: String? = null, // For educated
    val rating: Float = 0.0f, // Default rating
    val isVerified: Boolean = false
)