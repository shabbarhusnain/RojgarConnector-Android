package com.shabbar.rozgarconnector.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shabbar.rozgarconnector.R
import com.shabbar.rozgarconnector.adapters.ActivitiesAdapter
import com.shabbar.rozgarconnector.databinding.FragmentActivitiesBinding
import com.shabbar.rozgarconnector.models.ActivitiesModel
import com.shabbar.rozgarconnector.ui.job.JobPostActivity
import com.shabbar.rozgarconnector.ui.job.WorkPostActivity
import com.shabbar.rozgarconnector.ui.seeker.SeekerDetailActivity
import com.shabbar.rozgarconnector.utils.TranslatorUtil
import com.shabbar.rozgarconnector.ui.worker.WorkerDetailActivity

class ActivitiesFragment : Fragment() {

    private var _binding: FragmentActivitiesBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val allNotifications = mutableListOf<ActivitiesModel>()
    private val myPostedItems = mutableListOf<ActivitiesModel>()
    private val displayList = mutableListOf<Any>()
    private lateinit var adapter: ActivitiesAdapter
    private var currentFilter = "active"
    private var userRole = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilters()
        fetchUserRole()
        startRealtimeSync()
    }

    private fun fetchUserRole() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (isAdded && doc.exists()) {
                userRole = (doc.getString("role") ?: "seeker").lowercase()
                updateUIForRole()
            }
        }
    }

    private fun updateUIForRole() {
        if (!isAdded) return
        
        if (userRole == "seeker") {
            binding.chipMyJobs.visibility = View.VISIBLE
            binding.chipMyWorks.visibility = View.VISIBLE
            
            // Translate seeker chip labels
            binding.chipOffers.text = getString(R.string.sent_offers)
            binding.chipApplications.text = getString(R.string.worker_apps)
        } else {
            binding.chipMyJobs.visibility = View.GONE
            binding.chipMyWorks.visibility = View.GONE
            
            // Translate provider chip labels
            binding.chipOffers.text = getString(R.string.hire_requests)
            binding.chipApplications.text = getString(R.string.applied_jobs)
        }
        applyFilter("active")
    }

    private fun setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            when (checkedId) {
                R.id.chipActive -> applyFilter("active")
                R.id.chipMyJobs -> applyFilter("myjobs")
                R.id.chipMyWorks -> applyFilter("myworks")
                R.id.chipOffers -> applyFilter("hire")
                R.id.chipApplications -> applyFilter("job")
                R.id.chipHistory -> applyFilter("history")
            }
        }
    }

    private fun startRealtimeSync() {
        val uid = auth.currentUser?.uid ?: return

        // Sync Notifications (Contracts/Apps)
        db.collection("notifications").addSnapshotListener { snapshots, _ ->
            if (!isAdded) return@addSnapshotListener
            allNotifications.clear()
            snapshots?.forEach { doc ->
                val activity = doc.toObject(ActivitiesModel::class.java).apply { notificationId = doc.id }
                if (activity.receiverId == uid || activity.senderId == uid) allNotifications.add(activity)
            }
            refreshData()
        }

        // Sync My Posts (Jobs/Works)
        db.collection("jobs").whereEqualTo("seekerId", uid).addSnapshotListener { snapshots, _ ->
            if (!isAdded) return@addSnapshotListener
            myPostedItems.clear()
            snapshots?.forEach { doc ->
                val type = doc.getString("workerType") ?: "educated"
                val item = ActivitiesModel().apply {
                    notificationId = doc.id
                    taskTitle = doc.getString("jobTitle") ?: "Untitled"
                    budget = doc.getString("payAmount") ?: "0"
                    location = doc.getString("city") ?: "N/A"
                    status = doc.getString("status") ?: "open"
                    timestamp = doc.getTimestamp("timestamp")
                    viewsCount = doc.getLong("viewsCount")?.toInt() ?: 0 // POPULATE VIEWS COUNT
                    this.type = if (type == "educated") "myjob" else "mywork"
                }
                myPostedItems.add(item)
            }
            refreshData()
        }
    }

    private fun applyFilter(filter: String) {
        currentFilter = filter
        refreshData()
    }

    private fun refreshData() {
        if (!isAdded) return
        val uid = auth.currentUser?.uid ?: return
        displayList.clear()

        val filteredResult = when (currentFilter) {
            "myjobs" -> myPostedItems.filter { it.type == "myjob" && it.status == "open" }
            "myworks" -> myPostedItems.filter { it.type == "mywork" && it.status == "open" }
            
            "active" -> allNotifications.filter { 
                val status = it.status?.lowercase() ?: ""
                val isISeeker = if (it.type == "hire") it.senderId == uid else it.receiverId == uid
                val userFinished = if (isISeeker) it.seekerConfirmed else it.workerConfirmed
                (status == "accepted" || status == "approved") && !userFinished
            }
            "history" -> allNotifications.filter { 
                val status = it.status?.lowercase() ?: ""
                val isISeeker = if (it.type == "hire") it.senderId == uid else it.receiverId == uid
                val userFinished = if (isISeeker) it.seekerConfirmed else it.workerConfirmed
                status == "completed" || status == "rejected" || status == "cancelled" || ((status == "accepted" || status == "approved") && userFinished)
            }
            "hire" -> allNotifications.filter { it.type == "hire" && it.status == "pending" }
            "job" -> allNotifications.filter { it.type == "job" && it.status == "pending" }
            else -> emptyList()
        }

        groupAndShow(filteredResult)
    }

    private fun groupAndShow(list: List<ActivitiesModel>) {
        if (list.isEmpty()) { 
            adapter.updateData(emptyList())
            binding.llEmptyState.visibility = View.VISIBLE
            return 
        }
        
        binding.llEmptyState.visibility = View.GONE
        
        val header = when (currentFilter) {
            "myjobs" -> getString(R.string.header_my_jobs)
            "myworks" -> getString(R.string.header_my_works)
            "active" -> getString(R.string.header_active_contracts)
            "history" -> getString(R.string.header_history)
            "hire" -> if(userRole == "seeker") getString(R.string.header_sent_offers) else getString(R.string.header_hire_requests)
            "job" -> if(userRole == "seeker") getString(R.string.header_worker_apps) else getString(R.string.header_my_apps)
            else -> getString(R.string.notifications)
        }
        
        displayList.add(header)
        displayList.addAll(list.sortedByDescending { it.timestamp })
        adapter.updateData(displayList)
    }

    private fun setupRecyclerView() {
        adapter = ActivitiesAdapter(
            itemList = displayList,
            onNotificationClick = { activity ->
                if (activity.type == "myjob" || activity.type == "mywork") return@ActivitiesAdapter
                if (activity.status == "completed") showSummary(activity) else routeToUser(activity)
            },
            onEditJob = { activity ->
                val intent = if (activity.type == "myjob") Intent(requireContext(), JobPostActivity::class.java) 
                             else Intent(requireContext(), WorkPostActivity::class.java)
                intent.putExtra("EDIT_JOB_ID", activity.notificationId)
                startActivity(intent)
            },
            onDeleteJob = { activity ->
                AlertDialog.Builder(requireContext()).setTitle("Delete Post?").setMessage("Remove this post permanently?")
                    .setPositiveButton("DELETE") { _, _ -> db.collection("jobs").document(activity.notificationId!!).delete() }
                    .setNegativeButton("CANCEL", null).show()
            },
            onFinishClick = { activity -> showFinishDialog(activity) }
        )
        binding.rvActivities.layoutManager = LinearLayoutManager(requireContext())
        binding.rvActivities.adapter = adapter
    }

    private fun showFinishDialog(activity: ActivitiesModel) {
        val currentUid = auth.currentUser?.uid ?: return
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_job_completion, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog).setView(view).create()
        
        view.findViewById<Button>(R.id.btnSubmitCompletion).setOnClickListener {
            val rating = view.findViewById<RatingBar>(R.id.ratingBar).rating
            if (rating == 0f) return@setOnClickListener
            
            val updates = mutableMapOf<String, Any>()
            val isISeeker = if (activity.type == "hire") activity.senderId == currentUid else activity.receiverId == currentUid

            if (isISeeker) {
                updates["seekerConfirmed"] = true
                updates["ratingToWorker"] = rating
                updates["reviewToWorker"] = view.findViewById<EditText>(R.id.etReview).text.toString()
                if (activity.workerConfirmed) updates["status"] = "completed"
            } else {
                updates["workerConfirmed"] = true
                updates["ratingToSeeker"] = rating
                updates["reviewToSeeker"] = view.findViewById<EditText>(R.id.etReview).text.toString()
                if (activity.seekerConfirmed) updates["status"] = "completed"
            }
            
            db.collection("notifications").document(activity.notificationId!!).update(updates).addOnSuccessListener { dialog.dismiss() }
        }
        view.findViewById<Button>(R.id.btnDismiss).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun routeToUser(activity: ActivitiesModel) {
        val targetId = if (activity.senderId == auth.currentUser?.uid) activity.receiverId else activity.senderId
        if (targetId.isNullOrEmpty()) return
        db.collection("users").document(targetId).get().addOnSuccessListener { doc ->
            val role = (doc.getString("role") ?: "seeker").lowercase()
            if (role == "seeker") {
                startActivity(Intent(requireContext(), SeekerDetailActivity::class.java).apply { putExtra("SEEKER_ID", targetId) })
            } else {
                startActivity(Intent(requireContext(), WorkerDetailActivity::class.java).apply { 
                    putExtra("WORKER_ID", targetId)
                    if (activity.type == "job" && activity.status == "pending") {
                        putExtra("APPLICATION_ID", activity.notificationId)
                        putExtra("JOB_ID", activity.jobId)
                    }
                })
            }
        }
    }

    private fun showSummary(activity: ActivitiesModel) {
        AlertDialog.Builder(requireContext()).setTitle("Job Finished")
            .setMessage("Seeker Feedback: ${activity.reviewToWorker}\nWorker Feedback: ${activity.reviewToSeeker}")
            .setPositiveButton("OK", null).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}