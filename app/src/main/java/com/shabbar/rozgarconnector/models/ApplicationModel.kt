package com.shabbar.rozgarconnector.models

import com.google.firebase.Timestamp

data class ApplicationModel(
    var applicationId: String = "",
    var jobId: String = "",
    var jobTitle: String = "",
    var providerId: String = "",
    var seekerId: String = "",
    var workerName: String = "",
    var status: String = "pending",
    var timestamp: Timestamp? = null
)