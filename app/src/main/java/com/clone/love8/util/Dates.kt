package com.clone.love8.util

import com.clone.love8.data.AppData
import com.clone.love8.data.SpecialDay
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object Dates {
    private val displayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun parse(s: String?): LocalDate? = s?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    fun format(d: LocalDate): String = d.format(displayFmt)

    /** Ngày bắt đầu được tính là ngày thứ 1. */
    fun daysTogether(start: LocalDate, today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(start, today) + 1

    fun breakdown(start: LocalDate, today: LocalDate = LocalDate.now()): String {
        if (today.isBefore(start)) return "Sắp bắt đầu"
        val p = Period.between(start, today)
        val parts = mutableListOf<String>()
        if (p.years > 0) parts += "${p.years} năm"
        if (p.months > 0) parts += "${p.months} tháng"
        parts += "${p.days} ngày"
        return parts.joinToString(" ")
    }

    fun nextOccurrence(date: LocalDate, yearly: Boolean, today: LocalDate): LocalDate {
        if (!yearly) return date
        var d = date.withYear(today.year)
        if (d.isBefore(today)) d = date.withYear(today.year + 1)
        return d
    }

    fun daysUntil(target: LocalDate, today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, target)

    fun ddayLabel(target: LocalDate, today: LocalDate = LocalDate.now()): String {
        val diff = daysUntil(target, today)
        return when {
            diff == 0L -> "Hôm nay"
            diff > 0 -> "D-$diff"
            else -> "D+${-diff}"
        }
    }

    fun relative(ts: Long, now: Long = System.currentTimeMillis()): String {
        if (ts <= 0L) return "chưa có dữ liệu"
        val s = (now - ts) / 1000
        return when {
            s < 60 -> "vừa xong"
            s < 3600 -> "${s / 60} phút trước"
            s < 86400 -> "${s / 3600} giờ trước"
            else -> "${s / 86400} ngày trước"
        }
    }
}

data class UpcomingEvent(
    val key: String,
    val emoji: String,
    val title: String,
    val date: LocalDate,
    val remind: Boolean = true,
    val custom: SpecialDay? = null,
)

object Events {
    private val dayMarks: List<Long> = (1..10).map { it * 100L } + (3..40).map { it * 500L }

    fun all(d: AppData, today: LocalDate = LocalDate.now(), milestoneCount: Int = 4): List<UpcomingEvent> {
        val start = Dates.parse(d.startDate) ?: today
        val out = mutableListOf<UpcomingEvent>()

        dayMarks.map { it to start.plusDays(it - 1) }
            .filter { !it.second.isBefore(today) }
            .take(milestoneCount)
            .forEach { (m, date) -> out += UpcomingEvent("m$m", "💯", "$m ngày yêu nhau", date) }

        (1..60).map { it to start.plusYears(it.toLong()) }
            .filter { !it.second.isBefore(today) }
            .take(2)
            .forEach { (y, date) -> out += UpcomingEvent("y$y", "🎉", "Kỷ niệm $y năm yêu nhau", date) }

        Dates.parse(d.me.birthday)?.let {
            out += UpcomingEvent("bme", "🎂", "Sinh nhật ${d.me.name.ifBlank { "bạn" }}", Dates.nextOccurrence(it, true, today))
        }
        Dates.parse(d.partner.birthday)?.let {
            out += UpcomingEvent("bpartner", "🎂", "Sinh nhật ${d.partner.name.ifBlank { "người ấy" }}", Dates.nextOccurrence(it, true, today))
        }

        d.specialDays.forEach { sd ->
            val date = Dates.parse(sd.date) ?: return@forEach
            out += UpcomingEvent("c${sd.id}", sd.emoji, sd.title, Dates.nextOccurrence(date, sd.yearly, today), sd.remind, sd)
        }

        return out.sortedWith(
            compareBy<UpcomingEvent> { it.date.isBefore(today) }
                .thenBy { if (it.date.isBefore(today)) -it.date.toEpochDay() else it.date.toEpochDay() }
        )
    }
}
