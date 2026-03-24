package org.fossify.clock.views

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import android.widget.TextClock
import androidx.annotation.AttrRes
import org.fossify.clock.R
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.getFormattedDate
import org.fossify.commons.extensions.applyFontToTextView
import java.text.DateFormatSymbols
import java.util.Calendar
import androidx.core.content.withStyledAttributes

private const val AM_PM_SCALE = 0.4f

class MyTextClock @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = android.R.attr.textViewStyle,
) : TextClock(context, attrs, defStyleAttr) {

    init {
        if (!isInEditMode) context.applyFontToTextView(this)

        attrs?.let {
            context.withStyledAttributes(it, R.styleable.MyTextClock, defStyleAttr, 0) {
                useLocalizedDateFormat = getBoolean(R.styleable.MyTextClock_useLocalizedDateFormat, false)
            }
        }
    }

    private val amPmStrings by lazy {
        DateFormatSymbols.getInstance(
            resources.configuration.locales[0]
        ).amPmStrings
    }

    private var reenter = false
    private var useLocalizedDateFormat = false

    override fun setText(text: CharSequence?, type: BufferType?) {
        if (reenter) {
            super.setText(text, type)
            return
        }

        if (useLocalizedDateFormat) {
            val formattedDate = context.getFormattedDate(Calendar.getInstance())
            super.setText(formattedDate, type)
            return
        }

        if (context.config.use24HourFormat || text.isNullOrEmpty()) {
            super.setText(text, type)
            return
        }

        val amPmInfo = findAmPmInfo(text.toString())
        if (amPmInfo != null) {
            setTextWithAmPmScaled(text, amPmInfo, type)
        } else {
            super.setText(text, type)
        }
    }

    private fun findAmPmInfo(fullText: String): AmPmInfo? {
        for (s in amPmStrings) {
            if (s.isNotEmpty()) {
                val i = fullText.indexOf(s, ignoreCase = true)
                if (i != -1) {
                    return AmPmInfo(i, s)
                }
            }
        }
        return null
    }

    private fun setTextWithAmPmScaled(text: CharSequence?, amPmInfo: AmPmInfo, type: BufferType?) {
        val full = text.toString()
        val spannable = SpannableString(text)
        val startIndex = if (amPmInfo.position > 0 && full[amPmInfo.position - 1].isWhitespace()) {
            amPmInfo.position - 1
        } else {
            amPmInfo.position
        }
        val endIndex = amPmInfo.position + amPmInfo.string.length
        if (startIndex < endIndex) {
            spannable.setSpan(
                RelativeSizeSpan(AM_PM_SCALE),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        reenter = true
        try {
            super.setText(spannable, type ?: BufferType.SPANNABLE)
        } finally {
            reenter = false
        }
    }

    private data class AmPmInfo(val position: Int, val string: String)
}
