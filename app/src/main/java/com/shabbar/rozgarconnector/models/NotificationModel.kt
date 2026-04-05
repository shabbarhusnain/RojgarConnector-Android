package com.shabbar.rozgarconnector.models

import com.google.firebase.Timestamp

data class NotificationModel(
    var notificationId: String = "",
    var receiverId: String = "",
    var senderId: String = "",
    var senderName: String = "",
    var jobId: String = "",
    var jobTitle: String = "",
    var title: String = "",
    var message: String = "",
    var type: String = "",            // "hire" (Direct), "job" (Application)
    var status: String = "pending",   // "pending", "accepted", "completed", "cancelled"
    var timestamp: Timestamp? = null,
    var isRead: Boolean = false,

    // Professional Tracking
    var deadlineDate: Timestamp? = null,
    var completionDate: Timestamp? = null,
    var budget: String = "",
    var taskTitle: String = "",
    var location: String = "",
    
    // Mutual Confirmation Checklist Flags
    var seekerConfirmed: Boolean = false,
    var workerConfirmed: Boolean = false,
    var hasDamages: Boolean = false,        // If seeker reported damages
    var paymentFullReceived: Boolean = true, // If worker reported full payment
    var behaviorGood: Boolean = true,       // If worker reported good behavior
    
    // Reviews & Ratings
    var ratingToWorker: Float = 0f,
    var reviewToWorker: String = "",
    var ratingToSeeker: Float = 0f,
    var reviewToSeeker: String = ""
)