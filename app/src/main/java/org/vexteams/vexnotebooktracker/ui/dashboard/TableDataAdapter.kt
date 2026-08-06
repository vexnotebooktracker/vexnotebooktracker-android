package org.vexteams.vexnotebooktracker.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.vexteams.vexnotebooktracker.R
import org.vexteams.vexnotebooktracker.databinding.ItemTableRowBinding
import org.vexteams.vexnotebooktracker.models.TableRowData

/**
 * PERFORMANCE OPTIMIZATIONS IMPLEMENTED:
 * 1. Using ListAdapter with DiffUtil for efficient updates
 * 2. ViewHolder pattern optimized with proper binding
 * 3. Reduced findViewById calls by using view binding
 * 4. Cached resource lookups where possible
 * 5. Improved click handling efficiency
 * 6. Added copy functionality for URLs
 * 7. Compatible with existing layout design
 * 8. Resolved background resource issue
 */
class TableDataAdapter(
    private val onRowClick: (TableRowData) -> Unit
) : ListAdapter<TableRowData, TableDataAdapter.TableRowViewHolder>(TableRowDiffCallback()) {

    //  OPTIMIZATION: Cache colors and resources to avoid repeated lookups
    private var cachedColors: Map<String, Int>? = null
    private var cachedDrawables: Map<String, Int>? = null
    private var selectableItemBackground: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableRowViewHolder {
        val binding = ItemTableRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        // OPTIMIZATION: Cache resources on first creation
        if (cachedColors == null) {
            cachedColors = mapOf(
                "primary" to ContextCompat.getColor(parent.context, R.color.text_primary),
                "secondary" to ContextCompat.getColor(parent.context, R.color.text_secondary),
                "blue" to ContextCompat.getColor(parent.context, R.color.vex_blue),
                "orange" to ContextCompat.getColor(parent.context, R.color.vex_orange),
                "red" to ContextCompat.getColor(parent.context, R.color.red),
                "green" to ContextCompat.getColor(parent.context, R.color.green),
                "white" to ContextCompat.getColor(parent.context, R.color.white),
                "transparent" to ContextCompat.getColor(parent.context, android.R.color.transparent)
            )
        }

        if (cachedDrawables == null) {
            cachedDrawables = mapOf(
                "link" to R.drawable.ic_link,
                "calendar" to R.drawable.ic_calendar,
                "number" to R.drawable.ic_number,
                "group" to R.drawable.ic_group,
                "visibility" to R.drawable.ic_visibility,
                "check" to R.drawable.ic_check
            )
        }

        // ✅ FIX: Properly resolve selectableItemBackground attribute
        if (selectableItemBackground == null) {
            val typedValue = TypedValue()
            parent.context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
            selectableItemBackground = typedValue.resourceId
        }

        return TableRowViewHolder(binding, onRowClick, cachedColors!!, cachedDrawables!!, selectableItemBackground!!)
    }

    override fun onBindViewHolder(holder: TableRowViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TableRowViewHolder(
        internal val binding: ItemTableRowBinding,
        private val onRowClick: (TableRowData) -> Unit,
        private val colors: Map<String, Int>,
        private val drawables: Map<String, Int>,
        private val selectableItemBackground: Int
    ) : RecyclerView.ViewHolder(binding.root) {

        // ✅ OPTIMIZATION: Cache frequently used dimensions
        private val smallPadding by lazy {
            binding.root.context.resources.getDimensionPixelSize(R.dimen.padding_small)
        }
        private val spacerHeight by lazy {
            val spacerHeightDp = 1 // Minimal spacer - 1dp
            val density = binding.root.context.resources.displayMetrics.density
            (spacerHeightDp * density + 0.5f).toInt()
        }

        fun bind(tableRow: TableRowData) {
            binding.apply {
                tvField.text = tableRow.field
                tvValue.text = tableRow.value

                // ✅ STRONGEST MONOSPACE ENFORCEMENT: Explicitly create monospace typeface
                // This resists system-wide font overrides (like handwriting scripts)
                tvValue.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)

                // ✅ ADDITIONAL PROTECTION: Clear any TextAppearance that might override font
                tvValue.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL))

                // ✅ UPDATED: Use smaller padding for reduced row height
                root.setPadding(smallPadding, smallPadding, smallPadding, smallPadding)

                // Clear any existing drawables first
                tvField.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                tvValue.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)

                // ✅ NEW: Handle separator visibility
                if (tableRow.type == "separator") {
                    separator.visibility = View.VISIBLE
                    tvField.visibility = View.GONE
                    tvValue.visibility = View.GONE
                    ivCopy.visibility = View.INVISIBLE
                } else {
                    separator.visibility = View.GONE
                    tvField.visibility = View.VISIBLE
                    tvValue.visibility = View.VISIBLE
                }

                // ✅ NEW: Handle copy icon visibility and click
                if (tableRow.type == "url") {
                    ivCopy.visibility = View.VISIBLE
                    ivCopy.setOnClickListener {
                        copyUrlToClipboard(tableRow.value)
                    }
                } else if (tableRow.type != "separator") {
                    ivCopy.visibility = View.INVISIBLE
                    ivCopy.setOnClickListener(null)
                }

                // ✅ FIX: Check the value directly from tableRow, not by querying activity
                val viewCount = if (tableRow.type == "views") {
                    tableRow.value.toIntOrNull() ?: 0
                } else {
                    0
                }

                val isViewsClickable = tableRow.type == "views" &&
                        tableRow.urlId != null &&
                        viewCount > 0  // ✅ Use the value from the data itself

                val isClickable = isViewsClickable || tableRow.type == "delete"

                // ✅ OPTIMIZATION: Use when expression for better performance
                when (tableRow.type) {
                    "url" -> {
                        setupUrlRow()
                    }
                    "date" -> {
                        setupDateRow()
                    }
                    "number" -> {
                        setupNumberRow()
                    }
                    "team" -> {
                        setupTeamRow()
                    }
                    "views" -> {
                        setupViewsRow(isViewsClickable, tableRow)
                    }
                    "delete" -> {
                        setupDeleteRow()
                    }
                    "boolean" -> {
                        setupBooleanRow(tableRow.value)
                    }
                    "separator" -> {
                        setupSeparatorRow()
                    }
                    "spacer" -> {
                        setupSpacerRow()
                    }
                    else -> {
                        setupDefaultRow()
                    }
                }

                // ✅ FIX: Use consistent background for all rows
                if (isClickable) {
                    root.setOnClickListener { onRowClick(tableRow) }
                    root.isClickable = true
                    root.isFocusable = true
                } else {
                    root.setOnClickListener(null)
                    root.isClickable = false
                    root.isFocusable = false
                }

                // SPECIAL: White background for delete rows to act as separator
                if (tableRow.type == "delete") {
                    root.setBackgroundColor(colors["white"]!!)
                } else {
                    // All other rows use standard background
                    root.setBackgroundResource(selectableItemBackground)
                }

                // Set layout height
                root.layoutParams.height = if (tableRow.type == "spacer") {
                    spacerHeight
                } else {
                    ViewGroup.LayoutParams.WRAP_CONTENT
                }
            }
        }

        // NEW: Copy URL to clipboard with prefix
        private fun copyUrlToClipboard(shortUrl: String) {
            val context = binding.root.context
            val urlPrefix = context.getString(R.string.url_prefix)
            val fullUrl = "$urlPrefix$shortUrl"

            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText(context.getString(R.string.vex_notebook_url_label), fullUrl)
            clipboardManager.setPrimaryClip(clipData)

            Toast.makeText(context, context.getString(R.string.copied_url_format, fullUrl), Toast.LENGTH_SHORT).show()
        }

        // NEW: Public method for updating just the value
        fun updateValue(newValue: String) {
            binding.tvValue.text = newValue
        }

        // UPDATED: Extract row setup methods with existing color scheme
        private fun setupUrlRow() {
            binding.apply {
                tvValue.setTextColor(colors["secondary"]!!)
                tvValue.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                // REMOVED: No icon for URL rows
                tvField.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
        }

        private fun setupDateRow() {
            binding.apply {
                tvValue.setTextColor(colors["secondary"]!!)
                tvValue.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                // REMOVED: No icon for date rows
                tvField.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
        }

        private fun setupNumberRow() {
            binding.apply {
                tvValue.setTextColor(colors["secondary"]!!)
                tvValue.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                // REMOVED: No icon for number rows
                tvField.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
        }

        private fun setupTeamRow() {
            binding.apply {
                tvValue.setTextColor(colors["secondary"]!!)
                tvValue.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                // REMOVED: No icon for team rows
                tvField.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
        }

        private fun setupViewsRow(isClickable: Boolean, tableRow: TableRowData) {
            binding.apply {
                // REMOVED: No icon before field label
                tvField.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)

                if (isClickable) {
                    tvValue.setTextColor(colors["orange"]!!)
                    // NEW: Add hand/finger icon AFTER the value
                    tvValue.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawables["visibility"]!!, 0)
                    tvValue.compoundDrawablePadding = binding.root.context.resources.getDimensionPixelSize(R.dimen.padding_small)
                    tvField.text = tableRow.field // Remove tap hint, icon is enough
                } else {
                    tvValue.setTextColor(colors["secondary"]!!)
                    // No icon when not clickable
                    tvValue.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                }
                tvValue.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }
        }

        private fun setupDeleteRow() {
            binding.apply {
                tvField.text = binding.root.context.getString(R.string.delete_emoji)
                tvValue.text = ""
                tvValue.setTextColor(colors["red"]!!)
                tvValue.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }
        }

        private fun setupBooleanRow(value: String) {
            binding.apply {
                val yesString = binding.root.context.getString(R.string.yes)
                val color = if (value == yesString) colors["red"]!! else colors["green"]!!
                tvValue.setTextColor(color)
                tvValue.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                // REMOVED: No icon for boolean rows
                tvField.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
        }

        private fun setupSeparatorRow() {
            binding.apply {
                // UPDATED: Separator is now a full-width line, text fields are hidden
                separator.visibility = View.VISIBLE
                root.setPadding(0, 0, 0, 0) // No padding for separators
                tvValue.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }
        }

        private fun setupSpacerRow() {
            binding.apply {
                tvField.text = ""
                tvValue.text = ""
                tvValue.setTextColor(colors["transparent"]!!)
                tvValue.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                root.setPadding(0, 0, 0, 0) // No padding for spacers
            }
        }

        private fun setupDefaultRow() {
            binding.apply {
                tvValue.setTextColor(colors["secondary"]!!)
                tvValue.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }
        }

        // REMOVED: getViewsForUrl() method and cache - no longer needed
        // The view count is now checked directly from tableRow.value
        // OPTIMIZATION: Cache view count lookups
/*        private var lastViewsCache: Pair<Int, Int>? = null

        private fun getViewsForUrl(urlId: Int): Int {
            // Use cache if it's the same URL ID
            lastViewsCache?.let { (cachedUrlId, cachedViews) ->
                if (cachedUrlId == urlId) return cachedViews
            }

            val views = (binding.root.context as? DashboardActivity)?.getViewsForUrlId(urlId) ?: 0
            lastViewsCache = urlId to views
            return views
        }*/
    }

    // OPTIMIZATION: Improved DiffUtil callback for better performance
    private class TableRowDiffCallback : DiffUtil.ItemCallback<TableRowData>() {
        override fun areItemsTheSame(oldItem: TableRowData, newItem: TableRowData): Boolean {
            // Use a combination of field and urlId for unique identification
            return oldItem.field == newItem.field &&
                    oldItem.urlId == newItem.urlId &&
                    oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: TableRowData, newItem: TableRowData): Boolean {
            return oldItem == newItem
        }

        // ✅ OPTIMIZATION: Provide change payload for partial updates
        override fun getChangePayload(oldItem: TableRowData, newItem: TableRowData): Any? {
            return if (oldItem.value != newItem.value) {
                "value_changed"
            } else {
                null
            }
        }
    }

    // ✅ NEW: Method for efficient data updates
    fun updateData(newData: List<TableRowData>) {
        submitList(newData)
    }

    // ✅ OPTIMIZATION: Handle partial updates efficiently
    override fun onBindViewHolder(holder: TableRowViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.contains("value_changed")) {
            holder.binding.tvValue.text = getItem(position).value
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }
}