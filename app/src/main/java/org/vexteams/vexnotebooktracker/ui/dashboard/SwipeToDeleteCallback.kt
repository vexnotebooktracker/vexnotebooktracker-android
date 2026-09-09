package org.vexteams.vexnotebooktracker.ui.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.vexteams.vexnotebooktracker.R
import kotlin.math.abs
import kotlin.math.min

/**
 * Swipe a dashboard card to the left to delete it.
 *
 * This is the framework's own swipe-to-dismiss ([ItemTouchHelper]): the card follows the finger
 * over a red panel with a trash icon and, once released past the threshold, is carried off and
 * reported through [onSwipedLeft]. The caller is expected to offer an Undo rather than a
 * confirmation prompt, which is Material's guidance for a destructive swipe.
 */
class SwipeToDeleteCallback(
    context: Context,
    private val onSwipedLeft: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val background = Paint().apply {
        color = ContextCompat.getColor(context, R.color.red)
        isAntiAlias = true
    }

    private val icon = ContextCompat.getDrawable(context, R.drawable.ic_delete)?.mutate()?.apply {
        setTint(Color.WHITE)
    }

    private val cornerRadius = 6f * context.resources.displayMetrics.density
    private val iconMargin = 20f * context.resources.displayMetrics.density

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // adapterPosition rather than bindingAdapterPosition: the project resolves
        // androidx.recyclerview transitively at 1.1.0, which predates the newer accessor.
        val position = viewHolder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
            onSwipedLeft(position)
        }
    }

    /** Require a deliberate swipe - the cards are short, so the default 0.5 is easy to trip. */
    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.45f

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
            // The panel is only as wide as the card has actually moved, so it appears to be
            // revealed from underneath rather than sliding in.
            val panel = RectF(
                itemView.right + dX,
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat()
            )
            canvas.drawRoundRect(panel, cornerRadius, cornerRadius, background)

            icon?.let {
                val swipeDistance = abs(dX)
                // Fade the icon in over the first part of the swipe so a stray touch does not
                // flash a delete affordance.
                it.alpha = (min(swipeDistance / (iconMargin * 3f), 1f) * 255).toInt()

                if (swipeDistance > it.intrinsicWidth + iconMargin) {
                    val top = itemView.top + (itemView.height - it.intrinsicHeight) / 2
                    val right = (itemView.right - iconMargin).toInt()
                    it.setBounds(
                        right - it.intrinsicWidth,
                        top,
                        right,
                        top + it.intrinsicHeight
                    )
                    it.draw(canvas)
                }
            }
        }

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
