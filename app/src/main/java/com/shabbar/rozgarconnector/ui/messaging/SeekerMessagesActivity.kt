package com.shabbar.rozgarconnector.ui.messaging

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.shabbar.rozgarconnector.adapters.MessageAdapter
import com.shabbar.rozgarconnector.databinding.ActivitySeekerMessagesBinding
import com.shabbar.rozgarconnector.models.MessageModel

class SeekerMessagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeekerMessagesBinding
    private lateinit var adapter: MessageAdapter
    private val messageList = ArrayList<MessageModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeekerMessagesBinding.inflate(layoutInflater)
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
        messageList.add(MessageModel("Ali Khan", "I am interested in the plumbing job.", "10:00 AM"))
        messageList.add(MessageModel("Fatima Ahmed", "Can you share more details about the electrician job?", "11:30 AM"))
        messageList.add(MessageModel("Hassan Raza", "I have 5 years of experience in carpentry.", "Yesterday"))
        messageList.add(MessageModel("Ayesha Malik", "I am available for the painting job.", "2 days ago"))
        adapter.notifyDataSetChanged()
    }
}