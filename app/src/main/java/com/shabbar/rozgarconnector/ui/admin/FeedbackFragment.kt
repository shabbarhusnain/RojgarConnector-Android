package com.shabbar.rozgarconnector.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.shabbar.rozgarconnector.databinding.FragmentAdminFeedbackBinding
import com.shabbar.rozgarconnector.databinding.ItemFeedbackAdminBinding
import com.shabbar.rozgarconnector.models.ActivitiesModel

class FeedbackFragment : Fragment() {

    private var _binding: FragmentAdminFeedbackBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val feedbackList = mutableListOf<ActivitiesModel>()
    private lateinit var adapter: FeedbackAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FeedbackAdapter(feedbackList)
        binding.rvFeedbacks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFeedbacks.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadFeedbacks() }
        loadFeedbacks()
    }

    private fun loadFeedbacks() {
        binding.swipeRefresh.isRefreshing = true
        // Get all notifications where status is completed (implies feedback was given)
        db.collection("notifications")
            .whereEqualTo("status", "completed")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (!isAdded) return@addSnapshotListener
                binding.swipeRefresh.isRefreshing = false
                
                if (error != null) return@addSnapshotListener

                feedbackList.clear()
                snapshots?.forEach { doc ->
                    val notif = doc.toObject(ActivitiesModel::class.java)
                    // Check if there is actual feedback content
                    if (notif.reviewToWorker.isNotEmpty() || notif.reviewToSeeker.isNotEmpty()) {
                        feedbackList.add(notif)
                    }
                }
                adapter.notifyDataSetChanged()
                binding.lytNoFeedback.visibility = if (feedbackList.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class FeedbackAdapter(private val list: List<ActivitiesModel>) : RecyclerView.Adapter<FeedbackAdapter.FeedbackVH>() {
        inner class FeedbackVH(val b: ItemFeedbackAdminBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackVH {
            return FeedbackVH(ItemFeedbackAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: FeedbackVH, position: Int) {
            val item = list[position]
            holder.b.apply {
                tvJobTitle.text = item.jobTitle
                
                // Show Seeker's Feedback to Worker
                if (item.reviewToWorker.isNotEmpty()) {
                    tvSeekerReview.visibility = View.VISIBLE
                    tvSeekerReview.text = "Seeker: ${item.reviewToWorker}"
                    rbWorker.rating = item.ratingToWorker
                    rbWorker.visibility = View.VISIBLE
                } else {
                    tvSeekerReview.visibility = View.GONE
                    rbWorker.visibility = View.GONE
                }

                // Show Worker's Feedback to Seeker
                if (item.reviewToSeeker.isNotEmpty()) {
                    tvWorkerReview.visibility = View.VISIBLE
                    tvWorkerReview.text = "Worker: ${item.reviewToSeeker}"
                    rbSeeker.rating = item.ratingToSeeker
                    rbSeeker.visibility = View.VISIBLE
                } else {
                    tvWorkerReview.visibility = View.GONE
                    rbSeeker.visibility = View.GONE
                }
            }
        }

        override fun getItemCount() = list.size
    }
}