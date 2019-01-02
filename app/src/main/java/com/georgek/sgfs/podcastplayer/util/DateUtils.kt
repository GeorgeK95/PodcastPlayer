package com.georgek.sgfs.podcastplayer.util

import android.icu.lang.UProperty.DASH
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class DateUtils {
    companion object DateFormatter {
        val DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"
        val XML_DATE_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z"
        val DASH = "-"

        fun jsonDateToShortDate(jsonDate: String?): String {
            if (jsonDate == null) return DASH

            val inFormat = SimpleDateFormat(DATE_PATTERN)

            val date = inFormat.parse(jsonDate)

            val outputFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())

            return outputFormat.format(date)
        }

        fun xmlDateToDate(date: String?): Date {
            val date = date ?: return Date()
            val inFormat = SimpleDateFormat(XML_DATE_PATTERN)
            return inFormat.parse(date)
        }

        fun dateToShortDate(date: Date): String {
            val outputFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
            return outputFormat.format(date)
        }
    }
}