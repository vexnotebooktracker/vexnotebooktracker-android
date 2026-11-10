package com.bodepudi.vexnotebooktracker.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bodepudi.vexnotebooktracker.R
import com.bodepudi.vexnotebooktracker.databinding.ItemDashboardRecordBinding
import com.bodepudi.vexnotebooktracker.network.DashboardUrlRecord

/**
 * One card per notebook record.
 *
 * Replaces TableDataAdapter, which flattened every record into nine label/value rows. A record is
 * now a single adapter item, which is what makes swipe-to-delete and the alternating card tint
 * possible - both operate on an adapter position.
 */
class DashboardRecordAdapter(
    private val onViewsClick: (DashboardUrlRecord) -> Unit,
    private val formatEndDate: (String) -> String
) : ListAdapter<DashboardUrlRecord, DashboardRecordAdapter.RecordViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemDashboardRecordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RecordViewHolder(binding, onViewsClick, formatEndDate)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class RecordViewHolder(
        private val binding: ItemDashboardRecordBinding,
        private val onViewsClick: (DashboardUrlRecord) -> Unit,
        private val formatEndDate: (String) -> String
    ) : RecyclerView.ViewHolder(binding.root) {

        private val context: Context get() = binding.root.context

        fun bind(record: DashboardUrlRecord, position: Int) {
            binding.apply {
                // Monospace is set in code as well as XML: a system-wide font override (a
                // handwriting theme, say) can otherwise win over the layout attribute.
                val mono = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                tvShortUrlValue.typeface = mono
                tvTeamValue.typeface = mono
                tvViewsValue.typeface = mono
                tvEndDateValue.typeface = mono

                tvShortUrlValue.text = record.shortUrl
                tvTeamValue.text = record.team.trim()
                tvViewsValue.text = record.noOfViews.toString()
                tvEndDateValue.text = formatEndDate(record.endDate)

                // Alternating tint so each card reads as a separate record once the separator
                // rows are gone.
                val background = if (position % 2 == 0) R.color.white else R.color.vex_light_gray
                cardRoot.setCardBackgroundColor(ContextCompat.getColor(context, background))

                ivCopy.setOnClickListener { copyUrlToClipboard(record.shortUrl) }

                bindViews(record)
            }
        }

        /** The view count opens the view log, but only when there is something to show. */
        private fun bindViews(record: DashboardUrlRecord) = binding.apply {
            if (record.noOfViews > 0) {
                tvViewsValue.setTextColor(ContextCompat.getColor(context, R.color.vex_orange))
                tvViewsValue.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, R.drawable.ic_visibility, 0
                )
                tvViewsValue.isClickable = true
                tvViewsValue.setOnClickListener { onViewsClick(record) }
            } else {
                tvViewsValue.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                tvViewsValue.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                tvViewsValue.isClickable = false
                tvViewsValue.setOnClickListener(null)
            }
        }

        private fun copyUrlToClipboard(shortUrl: String) {
            val fullUrl = context.getString(R.string.url_prefix) + shortUrl

            val clipboardManager =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(
                ClipData.newPlainText(context.getString(R.string.vex_notebook_url_label), fullUrl)
            )

            Toast.makeText(
                context,
                context.getString(R.string.copied_url_format, fullUrl),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<DashboardUrlRecord>() {
        override fun areItemsTheSame(old: DashboardUrlRecord, new: DashboardUrlRecord) =
            old.urlId == new.urlId

        override fun areContentsTheSame(old: DashboardUrlRecord, new: DashboardUrlRecord) =
            old == new
    }
}
