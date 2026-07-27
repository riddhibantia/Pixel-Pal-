package com.pixelpal.app.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class SpeechBubbleOverlayView(context: Context) : FrameLayout(context) {

    val textView = TextView(context)
    val actionRow = LinearLayout(context)

    private val handler = Handler(Looper.getMainLooper())
    private var typewriterRunnable: Runnable? = null

    init {
        val density = resources.displayMetrics.density
        val paddingPx = (12 * density).toInt()

        val backgroundDrawable = GradientDrawable().apply {
            setColor(Color.parseColor("#2D2D44"))
            setStroke((2 * density).toInt(), Color.parseColor("#00D4AA"))
            cornerRadius = 12 * density
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            background = backgroundDrawable
        }

        textView.apply {
            setTextColor(Color.parseColor("#DFE6E9"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        }
        container.addView(textView)

        actionRow.apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            visibility = View.GONE
            setPadding(0, (8 * density).toInt(), 0, 0)
        }
        container.addView(actionRow)

        val params = LayoutParams(
            (220 * density).toInt(),
            LayoutParams.WRAP_CONTENT
        )
        container.layoutParams = params
        addView(container)
    }

    fun showText(text: String, onComplete: (() -> Unit)? = null) {
        typewriterRunnable?.let { handler.removeCallbacks(it) }
        textView.text = ""

        var index = 0
        typewriterRunnable = object : Runnable {
            override fun run() {
                if (index <= text.length) {
                    textView.text = text.substring(0, index++)
                    handler.postDelayed(this, 30L)
                } else {
                    onComplete?.invoke()
                }
            }
        }
        handler.post(typewriterRunnable!!)
    }

    fun showActions(
        onDone: () -> Unit,
        onSnooze: () -> Unit,
        onDismiss: () -> Unit
    ) {
        actionRow.removeAllViews()
        actionRow.visibility = View.VISIBLE

        val doneBtn = createButton("✓ Done", "#55EFC4") { onDone() }
        val snoozeBtn = createButton("⏰ 15m", "#FDCB6E") { onSnooze() }
        val dismissBtn = createButton("✕", "#FF7675") { onDismiss() }

        actionRow.addView(doneBtn)
        actionRow.addView(snoozeBtn)
        actionRow.addView(dismissBtn)
    }

    fun hideActions() {
        actionRow.visibility = View.GONE
    }

    private fun createButton(text: String, colorHex: String, onClick: () -> Unit): Button {
        val density = resources.displayMetrics.density
        return Button(context).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(Color.parseColor(colorHex))
            setBackgroundColor(Color.TRANSPARENT)
            setPadding((4 * density).toInt(), 0, (4 * density).toInt(), 0)
            setOnClickListener { onClick() }
        }
    }
}
