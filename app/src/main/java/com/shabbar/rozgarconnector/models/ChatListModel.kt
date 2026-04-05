package com.shabbar.rozgarconnector.models

data class ChatListModel(
    val userId: String = "",
    val userName: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0,
    val profileImage: String? = null,
    var hasUnread: Boolean = false // Flag for Red Dot
)