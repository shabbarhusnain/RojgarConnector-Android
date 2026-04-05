package com.shabbar.rozgarconnector.models

import com.google.firebase.Timestamp

data class JobModel(
    var jobId: String = "",
    var seekerId: String = "",
    var jobTitle: String = "",
    var workplaceName: String = "",
    var workplaceType: String = "",
    var workplaceAddress: String = "",
    var district: String = "",
    var province: String = "Punjab",
    var payAmount: String = "",
    var payUnit: String = "",
    var durationValue: String = "",
    var durationUnit: String = "",
    var jobDescription: String = "",
    var workerType: String = "", 
    var category: String = "",
    var status: String = "open",
    var timestamp: Timestamp? = null, // Changed from Long to Timestamp?
    var applicants: List<String> = arrayListOf()
)