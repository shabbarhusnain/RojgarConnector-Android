package com.shabbar.rozgarconnector.ui.messaging

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.shabbar.rozgarconnector.adapters.MessageAdapter
import com.shabbar.rozgarconnector.databinding.ActivityProviderMessagesBinding
import com.shabbar.rozgarconnector.models.MessageModel

class ProviderMessagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProviderMessagesBinding
    private lateinit var adapter: MessageAdapter
    private val messageList = ArrayList<MessageModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProviderMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadDummyMessages()
    }

    private fun setupRecyclerView() {
        binding.messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MessageAdapter(messageList)
        binding.messagesRecyclerView.adapter = adapter
    }

    private fun loadDummyMessages() {
        messageList.add(MessageModel("Service Seeker 1", "I have a plumbing job for you.", "10:00 AM"))
        messageList.add(MessageModel("Service Seeker 2", "Are you available for an electrician job?", "11:30 AM"))
        messageList.add(MessageModel("Service Seeker 3", "I need a carpenter for a new project.", "Yesterday"))
        messageList.add(MessageModel("Service Seeker 4", "I have a painting job.", "2 days ago"))
        adapter.notifyDataSetChanged()
    }
}