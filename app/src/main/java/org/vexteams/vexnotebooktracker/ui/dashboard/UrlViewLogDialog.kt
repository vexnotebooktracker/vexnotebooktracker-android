package org.vexteams.vexnotebooktracker.ui.dashboard

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
import org.vexteams.vexnotebooktracker.R
import org.vexteams.vexnotebooktracker.databinding.DialogUrlViewLogBinding
import org.vexteams.vexnotebooktracker.network.UrlViewLogEntry

class UrlViewLogDialog(
    context: Context,
    private val title: String,
    private val entries: List<UrlViewLogEntry>,
    private val onEntryClick: (UrlViewLogEntry) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogUrlViewLogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DialogUrlViewLogBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        setupDialog()
        setupRecyclerView()
    }

    private fun setupDialog() {
        binding.tvTitle.text = title
        binding.tvSubtitle.text = context.getString(R.string.view_entries_count, entries.size)

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Make dialog take up most of the screen
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            (context.resources.displayMetrics.heightPixels * 0.7).toInt()
        )
    }

    private fun setupRecyclerView() {
        val adapter = UrlViewLogAdapter(entries) { entry ->
            onEntryClick(entry)
            dismiss() // Close this dialog when an entry is clicked
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }
    }

    companion object {
        fun show(
            context: Context,
            title: String,
            entries: List<UrlViewLogEntry>,
            onEntryClick: (UrlViewLogEntry) -> Unit
        ) {
            UrlViewLogDialog(context, title, entries, onEntryClick).show()
        }
    }
}