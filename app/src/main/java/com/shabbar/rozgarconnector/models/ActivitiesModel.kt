package com.shabbar.rozgarconnector.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class ActivitiesModel(
    var notificationId: String? = "",
    var receiverId: String? = "",
    var senderId: String? = "",
    var senderName: String? = "",
    var jobId: String? = "",
    var jobTitle: String? = "",
    var title: String? = "",
    var message: String? = "",
    var type: String? = "",            // "hire" (Direct), "job" (Application)
    var status: String? = "pending",   // "pending", "accepted", "completed", "cancelled", "disputed"
    var timestamp: Timestamp? = null,
    
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,

    // Professional Tracking
    var deadlineDate: Timestamp? = null,
    var completionDate: Timestamp? = null,
    var budget: String? = "",
    var taskTitle: String? = "",
    var location: String? = "",
    
    // Mutual Confirmation Checklist Flags
    var seekerConfirmed: Boolean = false,
    var workerConfirmed: Boolean = false,
    
    // Step 4: Dispute & Damage Logic
    var hasDamages: Boolean = false,        // Seeker report: Physical damage
    var taskNotCompleted: Boolean = false,  // Seeker report: Job unfinished
    var paymentFullReceived: Boolean = true, // Worker report: Payment issue
    var behaviorGood: Boolean = true,       // Worker report: Conduct issue
    var disputeReason: String? = "",         // Detailed reason if status is "disputed"
    
    // Reviews & Ratings
    var ratingToWorker: Float = 0f,
    var reviewToWorker: String? = "",
    var ratingToSeeker: Float = 0f,
    var reviewToSeeker: String? = ""
)