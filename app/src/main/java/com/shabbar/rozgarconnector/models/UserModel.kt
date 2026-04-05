package com.shabbar.rozgarconnector.models

data class UserModel(
    var uid: String = "",
    val fullName: String = "",
    val fatherName: String = "",
    val phone: String = "",
    val cnic: String = "",
    val district: String = "",
    val city: String? = null,
    val permanentAddress: String? = null,
    val dob: String? = null,
    val role: String = "",            // "Admin", "Seeker", "Worker"
    val workerType: String = "",      // "educated", "uneducated", "none"
    val profileImageUrl: String? = null,
    val isVerified: Boolean = false,
    val profileCompleted: Boolean = false,
    
    // Base64 fields
    val dpBase64: String? = null,
    val cnicFrontBase64: String? = null,
    val cnicBackBase64: String? = null,
    
    // Educated Profile Fields (Portfolio)
    val lastDegree: String? = null,      // Metric, Inter, BS, etc.
    val degreeName: String? = null,      // Title like "Software Engineering"
    val boardUniversity: String? = null,
    val percentageCGPA: String? = null,
    val professionalSkill: String? = null,
    val experienceYears: String? = null,
    val lastWorkPlace: String? = null,
    val professionalDescription: String? = null,
    val degreePhotoBase64: String? = null,
    
    // Uneducated Fields
    val skills: String? = null,
    val experience: String? = null,
    val dailyRate: String? = null,

    // Rating Fields
    val totalRating: Float = 0f,
    val ratingCount: Int = 0,
    val averageRating: Float = 0f
)