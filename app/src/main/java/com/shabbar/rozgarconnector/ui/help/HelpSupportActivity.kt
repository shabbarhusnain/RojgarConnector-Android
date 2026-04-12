package com.shabbar.rozgarconnector.ui.help

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.databinding.ActivityHelpSupportBinding

class HelpSupportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpSupportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpSupportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFaqs()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnContactSupport.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@rozgarconnector.com")
                putExtra(Intent.EXTRA_SUBJECT, "Support Request")
            }
            try {
                startActivity(Intent.createChooser(emailIntent, "Send Email Using..."))
            } catch (e: Exception) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupFaqs() {
        // Category 1: Account
        setFaqText(binding.faq1.root, R.string.faq_q1, R.string.faq_a1)
        setFaqText(binding.faq2.root, R.string.faq_q2, R.string.faq_a2)
        setFaqText(binding.faq3.root, R.string.faq_q3, R.string.faq_a3)
        setFaqText(binding.faq4.root, R.string.faq_q4, R.string.faq_a4)

        // Category 2: Seeker
        setFaqText(binding.faq5.root, R.string.faq_q5, R.string.faq_a5)
        setFaqText(binding.faq6.root, R.string.faq_q6, R.string.faq_a6)
        setFaqText(binding.faq7.root, R.string.faq_q7, R.string.faq_a7)
        setFaqText(binding.faq8.root, R.string.faq_q8, R.string.faq_a8)

        // Category 3: Provider
        setFaqText(binding.faq9.root, R.string.faq_q9, R.string.faq_a9)
        setFaqText(binding.faq10.root, R.string.faq_q10, R.string.faq_a10)
        setFaqText(binding.faq11.root, R.string.faq_q11, R.string.faq_a11)
        setFaqText(binding.faq12.root, R.string.faq_q12, R.string.faq_a12)

        // Category 4: Safety
        setFaqText(binding.faq13.root, R.string.faq_q13, R.string.faq_a13)
        setFaqText(binding.faq14.root, R.string.faq_q14, R.string.faq_a14)
        setFaqText(binding.faq15.root, R.string.faq_q15, R.string.faq_a15)
        setFaqText(binding.faq16.root, R.string.faq_q16, R.string.faq_a16)

        // Category 5: Completion
        setFaqText(binding.faq17.root, R.string.faq_q17, R.string.faq_a17)
        setFaqText(binding.faq18.root, R.string.faq_q18, R.string.faq_a18)
        setFaqText(binding.faq19.root, R.string.faq_q19, R.string.faq_a19)
        setFaqText(binding.faq20.root, R.string.faq_q20, R.string.faq_a20)
    }

    private fun setFaqText(view: android.view.View, qRes: Int, aRes: Int) {
        view.findViewById<TextView>(R.id.tvQuestion).text = getString(qRes)
        view.findViewById<TextView>(R.id.tvAnswer).text = getString(aRes)
    }
}