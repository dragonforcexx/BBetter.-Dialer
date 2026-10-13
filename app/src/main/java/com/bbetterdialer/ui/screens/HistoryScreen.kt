package com.bbetterdialer.ui.screens

import com.bbetterdialer.ui.T

import android.provider.CallLog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbetterdialer.data.AppPreferences
import com.bbetterdialer.data.CallEntry
import com.bbetterdialer.data.Contact
import com.bbetterdialer.data.ContactOverrides
import com.bbetterdialer.ui.design.ContactAvatar
import com.bbetterdialer.ui.design.PillBadge
import com.bbetterdialer.ui.design.PlainCallIcon
import com.bbetterdialer.ui.formatDial
import com.bbetterdialer.ui.initialsOf
import com.bbetterdialer.ui.relativeTime
import com.bbetterdialer.ui.rememberContactPhoto
import com.bbetterdialer.ui.theme.EndRed
import com.bbetterdialer.ui.theme.Surface1
import com.bbetterdialer.ui.theme.TextPrimary
import com.bbetterdialer.ui.theme.TextSecondary
import com.bbetterdialer.ui.theme.TextTertiary
import com.bbetterdialer.ui.theme.VividGreen
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * ===========================================================================
 *  ELŐZMÉNYEK – napi bontásban, összevonva
 * ===========================================================================
 *  A napló nem soronként listázódik, hanem **időszakokra** bontva:
 *
 *      Mai nap  →  Tegnap  →  A héten  →  Régebbi
 *
 *  Egy időszakon belül minden hívó **egyszer** szerepel, mellette azzal, hogy
 *  hányszor beszéltünk vele (`6×`). Így egy pillantással látszik, kivel
 *  mennyit beszéltünk aznap / a héten.
 * ===========================================================================
 */

private data class HistoryItem(
    val key: String,
    val name: String?,
    val number: String,
    val photoUri: String?,
    val contactId: Long?,
    val count: Int,
    val missedCount: Int,
    val lastDateMillis: Long,
)

private data class HistorySection(
    val index: Int,
    val items: List<HistoryItem>
)

private class Aggregate {
    var name: String? = null
    var number: String = ""
    var photoUri: String? = null
    var contactId: Long? = null
    var count: Int = 0
    var missed: Int = 0
    var lastDate: Long = 0L
}

private const val SECTION_COUNT = 4

/** Az időszakok címei a választott nyelven. */
private fun sectionTitle(index: Int): String = when (index) {
    0 -> T.today
    1 -> T.yesterday
    2 -> T.thisWeek
    else -> T.older
}

private fun buildSections(entries: List<CallEntry>, contacts: List<Contact>): List<HistorySection> {
    if (entries.isEmpty()) return emptyList()

    val byNumber = contacts.associateBy { it.number.filter { ch -> ch.isDigit() }.takeLast(9) }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)

    val buckets = List(SECTION_COUNT) { LinkedHashMap<String, Aggregate>() }

    entries.forEach { entry ->
        val days = ChronoUnit.DAYS.between(
            Instant.ofEpochMilli(entry.dateMillis).atZone(zone).toLocalDate(),
            today
        )
        val bucketIndex = when {
            days <= 0L -> 0
            days == 1L -> 1
            days < 7L -> 2
            else -> 3
        }

        val digits = entry.number.filter { it.isDigit() }
        val contact = byNumber[digits.takeLast(9)]

        val key = contact?.contactId?.toString()
            ?: entry.name?.takeIf { it.isNotBlank() }?.trim()?.lowercase()
            ?: digits.takeLast(9).ifEmpty { entry.number }

        val aggregate = buckets[bucketIndex].getOrPut(key) { Aggregate() }
        aggregate.count += 1

        if (entry.type == CallLog.Calls.MISSED_TYPE ||
            entry.type == CallLog.Calls.REJECTED_TYPE ||
            entry.type == CallLog.Calls.BLOCKED_TYPE
        ) {
            aggregate.missed += 1
        }

        if (entry.dateMillis >= aggregate.lastDate) {
            aggregate.lastDate = entry.dateMillis
            aggregate.number = entry.number
        }
        if (aggregate.name == null) aggregate.name = contact?.name ?: entry.name
        if (aggregate.contactId == null) aggregate.contactId = contact?.contactId
        if (aggregate.photoUri == null) aggregate.photoUri = contact?.photoUri ?: entry.photoUri
    }

    return buckets.mapIndexedNotNull { index, bucket ->
        if (bucket.isEmpty()) return@mapIndexedNotNull null
        HistorySection(
            index = index,
            items = bucket.map { (key, aggregate) ->
                HistoryItem(
                    key = key,
                    name = aggregate.name,
                    number = aggregate.number,
                    photoUri = aggregate.photoUri,
                    contactId = aggregate.contactId,
                    count = aggregate.count,
                    missedCount = aggregate.missed,
                    lastDateMillis = aggregate.lastDate
                )
            }.sortedByDescending { it.lastDateMillis }
        )
    }
}

@Composable
fun HistoryScreen(
    entries: List<CallEntry>,
    contacts: List<Contact>,
    hasPermission: Boolean,
    onCall: (String) -> Unit,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!hasPermission) {
        HintCard(title = T.noPermissionCallLog, body = T.noPermissionCallLogBody)
        return
    }
    if (entries.isEmpty()) {
        HintCard(title = T.noCallsYet, body = T.noCallsYetBody)
        return
    }

    val sections = remember(entries, contacts, AppPreferences.language) {
        buildSections(entries, contacts)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sections.forEach { section ->
            item(key = "header_${section.index}") {
                SectionHeader(
                    title = sectionTitle(section.index),
                    callCount = section.items.sumOf { it.count }
                )
            }

            items(
                count = section.items.size,
                key = { index -> "${section.index}_${section.items[index].key}" }
            ) { index ->
                val item = section.items[index]
                HistoryRow(
                    item = item,
                    onCall = { onCall(item.number) },
                    onPick = { onPick(item.number) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, callCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = T.callsCount(callCount),
            color = TextTertiary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    onCall: () -> Unit,
    onPick: () -> Unit,
) {
    val displayName = ContactOverrides.nameFor(item.contactId, item.name)
    val photo = rememberContactPhoto(item.photoUri, item.contactId, sizePx = 192)
    val allMissed = item.count > 0 && item.missedCount == item.count
    val colorOverride = ContactOverrides.colorFor(item.contactId)
    val accent: Color = colorOverride ?: if (allMissed) EndRed else VividGreen

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .clickable(onClick = onPick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(
            image = photo,
            initials = initialsOf(displayName ?: item.number),
            size = 46.dp,
            accent = accent,
            colorOverride = colorOverride
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = displayName?.takeIf { it.isNotBlank() } ?: formatDial(item.number),
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = relativeTime(item.lastDateMillis) + " · " + formatDial(item.number),
                color = TextTertiary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Hányszor beszéltünk vele ebben az időszakban
        if (item.count > 1) {
            PillBadge(
                text = "${item.count}×",
                highlighted = true,
                fontSize = 11
            )
            Spacer(Modifier.width(6.dp))
        }

        PlainCallIcon(
            onClick = onCall,
            size = 44.dp,
            iconSize = 22.dp
        )
    }
}
