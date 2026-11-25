package com.example.util

import com.example.model.SaleLine
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {
    private val frLocale = Locale.FRENCH

    fun formatNumber(value: Long): String {
        val nf = NumberFormat.getIntegerInstance(frLocale)
        return nf.format(value).replace('\u00A0', ' ').replace('\u202F', ' ')
    }

    fun formatFcfa(amount: Long, currency: String = "FCFA"): String {
        return "${formatNumber(amount)} $currency"
    }

    fun formatDateTime(epochMs: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", frLocale)
        return sdf.format(Date(epochMs))
    }

    fun formatDate(epochMs: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", frLocale)
        return sdf.format(Date(epochMs))
    }

    fun formatMonthYear(year: Int, monthZeroIndexed: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroIndexed)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val sdf = SimpleDateFormat("MMMM yyyy", frLocale)
        val raw = sdf.format(cal.time)
        return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(frLocale) else it.toString() }
    }

    fun dayRangeMs(year: Int, monthZeroIndexed: Int, dayOfMonth: Int): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            set(year, monthZeroIndexed, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            set(year, monthZeroIndexed, dayOfMonth, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return start.timeInMillis to end.timeInMillis
    }

    fun monthRangeMs(year: Int, monthZeroIndexed: Int): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            set(year, monthZeroIndexed, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            set(year, monthZeroIndexed, 1, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        return start.timeInMillis to end.timeInMillis
    }

    fun buildSaleItemsSummary(lines: List<SaleLine>): String {
        val joined = lines.joinToString(", ") { "${it.quantity}x ${it.productName}" }
        return if (joined.length <= 500) joined else joined.take(497) + "..."
    }
}
