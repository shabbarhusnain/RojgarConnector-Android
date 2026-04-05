package com.shabbar.rozgarconnector.models

data class MessageModel(
    var messageId: String = "",
    var senderId: String = "",
    var receiverId: String = "",
    var message: String = "",
    var timestamp: Long = 0,
    var isRead: Boolean = false,
    var type: String = "text" // "text", "image", "location"
)