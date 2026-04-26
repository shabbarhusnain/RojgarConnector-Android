package com.shabbar.rozgarconnector.models

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class UserModel(
    var uid: String? = "",
    
    @get:PropertyName("fullName")
    @set:PropertyName("fullName")
    var fullName: String? = "",

    var fatherName: String? = "",
    var phone: String? = "",
    var cnic: String? = "",
    var district: String? = "",
    var city: String? = null,
    var permanentAddress: String? = null,
    var dob: String? = null,
    var role: String? = "",
    var workerType: String? = "",
    var profileImageUrl: String? = null,
    
    @get:PropertyName("isVerified")
    @set:PropertyName("isVerified")
    var isVerified: Boolean = false,

    @get:PropertyName("isRejected")
    @set:PropertyName("isRejected")
    var isRejected: Boolean = false,

    @get:PropertyName("isOnline")
    @set:PropertyName("isOnline")
    var isOnline: Boolean = false,
    
    @get:PropertyName("isBlocked")
    @set:PropertyName("isBlocked")
    var isBlocked: Boolean = false,
    
    var profileCompleted: Boolean = false,
    var dpBase64: String? = null,
    var cnicFrontBase64: String? = null,
    var cnicBackBase64: String? = null,
    var degreePhotoBase64: String? = null,
    
    // Portfolio Fields
    var lastDegree: String? = null,
    var degreeName: String? = null,
    var professionalSkill: String? = null,
    var experienceYears: String? = null,
    var lastWorkPlace: String? = null,
    var jobTitle: String? = null,
    var employmentDuration: String? = null,
    var certifications: String? = null,
    var professionalDescription: String? = null,
    var projectPhotoBase64: String? = null,
    
    // Skill specific
    var hasOwnTools: Boolean? = false,
    
    var skills: String? = null,
    var experience: String? = null,
    var dailyRate: String? = null,
    var totalRating: Float = 0f,
    var ratingCount: Int = 0,
    var averageRating: Float = 0f
)