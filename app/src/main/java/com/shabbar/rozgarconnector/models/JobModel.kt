package com.shabbar.rozgarconnector.models

import com.google.firebase.Timestamp

data class JobModel(
    var jobId: String? = "",
    var seekerId: String? = "",
    var jobTitle: String? = "",
    var workplaceName: String? = "",
    var workplaceType: String? = "",
    var workplaceAddress: String? = "",
    var workplaceDescription: String? = "",
    var district: String? = "",
    var province: String? = "Punjab",
    var payAmount: String? = "",
    var payUnit: String? = "",
    var durationValue: String? = "",
    var durationUnit: String? = "",
    var jobDescription: String? = "",
    var workerType: String? = "", 
    var category: String? = "",
    var status: String? = "open",
    var timestamp: Timestamp? = null,
    var applicants: List<String>? = arrayListOf(),
    
    // View Tracking
    var viewsCount: Int = 0,
    var viewedBy: List<String> = arrayListOf(),
    
    // Step 1: Ethical & Professional Enhancements
    var isNegotiable: Boolean = false,
    var toolsProvidedBy: String? = "Seeker", 
    var isVisitRequired: Boolean = false,
    var hasSafetyHazards: Boolean = false,
    var hazardsDescription: String? = "",
    var ethicalTermsAccepted: Boolean = true,

    // Additional fields from Firestore matching JobPostActivity
    var companyIntro: String? = "",
    var qualifications: String? = "",
    var lastDateToApply: String? = "",
    var jobPhotoBase64: String? = "",
    var benefits: String? = "",
    var jobAttachmentsBase64: List<String>? = arrayListOf(),
    var jobRequirements: String? = ""
)