package com.phenomena.study.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phenomena.study.data.*
import com.phenomena.study.model.Work

/**
 * A search-first, genuinely lazy library. None of the curriculum totals are
 * hard-coded, so new books and seminars immediately appear on the home page.
 */
@Composable
fun ModernLibraryScreen(
    catalog: Catalog,
    progress: List<LessonProgress>,
    onWork: (String) -> Unit,
    onLesson: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val completed = progress.count { it.completed }
    val filters = listOf("All", "Husserl", "Heidegger", "French", "Other")
    val matched = remember(query, filter, catalog.works) {
        catalog.works.filter { book ->
            val searchable = listOf(book.title, book.author, book.description, book.year).joinToString(" ")
            val termMatches = query.isBlank() || searchable.contains(query.trim(), ignoreCase = true)
            val strandMatches = when(filter) {
                "Husserl" -> book.author.contains("Husserl", ignoreCase = true)
                "Heidegger" -> book.author.contains("Heidegger", ignoreCase = true)
                "French" -> listOf("Merleau-Ponty", "Sartre", "Beauvoir", "Levinas",
                    "Derrida", "Ricoeur", "Henry", "Marion").any { book.author.contains(it, ignoreCase = true) }
                "Other" -> !(book.author.contains("Husserl", ignoreCase = true) ||
                    book.author.contains("Heidegger", ignoreCase = true) ||
                    listOf("Merleau-Ponty", "Sartre", "Beauvoir", "Levinas",
                        "Derrida", "Ricoeur", "Henry", "Marion").any { book.author.contains(it, ignoreCase = true) })
                else -> true
            }
            termMatches && strandMatches
        }
    }
    val resume = progress.filter { it.lastOpened > 0L }
        .maxByOrNull { it.lastOpened }
        ?.let { catalog.lesson(it.lessonId) }
        ?: catalog.lesson("bt05") ?: catalog.lessons.firstOrNull()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 19.dp, end = 19.dp, top = 22.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(19.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow("THE PHENOMENOLOGICAL ARCHIVE", Modifier.weight(1f))
                    Surface(color = Cream, shape = RoundedCornerShape(50.dp), border = BorderStroke(1.dp, Line)) {
                        Text("VOL. 01", Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 10.sp, color = Mint, letterSpacing = 1.sp)
                    }
                }
                Display("An atlas of\nappearing.", size = 45)
                Text("A quieter way to study difficult ideas.", color = Muted,
                    fontSize = 13.sp, lineHeight = 21.sp)
            }
        }
        item {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF244B49), Color(0xFF17252E), Color(0xFF222B33))))
            ) {
                Icon(Icons.Default.AutoStories, contentDescription = null, tint = Color.White.copy(alpha = .07f),
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 14.dp, y = (-8).dp).size(155.dp))
                Column(Modifier.fillMaxWidth().padding(21.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("THE STUDY COLLECTION", fontSize = 10.sp, color = Brass, letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold)
                    Display("The long conversation\nof consciousness.", size = 27, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        StatPill(catalog.works.size.toString(), "WORKS")
                        StatPill(catalog.lessons.size.toString(), "LESSONS")
                        StatPill(catalog.seminars.size.toString(), "SEMINARS")
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = .14f))
                    Text("From intentionality and the lifeworld to embodiment, ethics and existence.",
                        color = Color.White.copy(alpha = .78f), fontSize = 12.sp, lineHeight = 19.sp)
                }
            }
        }
        if (resume != null) item {
            Surface(onClick = { onLesson(resume.id) }, color = CardRaised,
                shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Line)) {
                Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                    Surface(color = Moss, shape = RoundedCornerShape(15.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null,
                            tint = Color.White, modifier = Modifier.padding(15.dp).size(25.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Eyebrow("CONTINUE YOUR READING")
                        Text(resume.title, color = Ink, fontSize = 16.sp, maxLines = 2,
                            lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, overflow = TextOverflow.Ellipsis)
                        Text("${catalog.work(resume.workId)?.author} · ${resume.section}",
                            fontSize = 11.sp, color = Muted)
                    }
                    Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = Mint,
                        modifier = Modifier.size(21.dp))
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    RowHeadline("Your collection")
                    Text("${catalog.works.size} scholarly study companions", color = Muted, fontSize = 12.sp)
                }
                Text("${completed} / ${catalog.lessons.size}", color = Brass,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (catalog.lessons.isEmpty()) 0f else completed.toFloat() / catalog.lessons.size },
                color = Mint, trackColor = Cream, modifier = Modifier.fillMaxWidth()
                    .height(4.dp).clip(RoundedCornerShape(3.dp))
            )
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Muted) },
                placeholder = { Text("Search authors, works and ideas…", fontSize = 13.sp) },
                trailingIcon = if (query.isNotBlank()) ({ IconButton(onClick = { query = "" }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                } }) else null,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp)
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(filters) { choice ->
                    LabelTag(choice, selected = choice == filter, onClick = { filter = choice })
                }
            }
        }
        item {
            RowHeadline("Explore the works", "${matched.size} TITLES")
        }
        if (matched.isEmpty()) item {
            Panel(Modifier.fillMaxWidth()) {
                Icon(Icons.Default.SearchOff, contentDescription = null, tint = Brass)
                AccentTitle("No works found")
                Body("Try a different author, title or philosophical concept.", color = Muted)
            }
        }
        items(matched, key = { it.id }) { work ->
            ModernWorkCard(work, catalog.lessons.count { it.workId == work.id }, onClick = { onWork(work.id) })
        }
        item {
            Text("PHENOMENA · ORIGINAL EDUCATIONAL COMMENTARY\n"
                + "Consult your authorized editions for complete primary texts.",
                color = Muted, fontSize = 10.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun StatPill(value: String, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(value, fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
        Text(label, fontSize = 9.sp, letterSpacing = 1.2.sp, color = Color.White.copy(alpha = .65f))
    }
}

@Composable
private fun ModernWorkCard(work: Work, lessons: Int, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = CardSurface,
        shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Line)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            Box(Modifier.width(89.dp).height(123.dp).clip(RoundedCornerShape(13.dp))
                .background(Brush.verticalGradient(listOf(Color(work.color), Color(0xFF172F35))))) {
                Icon(Icons.Default.AutoStories, null, tint = Color.White.copy(alpha = .13f),
                    modifier = Modifier.align(Alignment.Center).size(54.dp))
                Text(work.year, color = Color.White.copy(alpha = .75f), fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp))
                Text("P", color = Color.White, fontFamily = FontFamily.Serif,
                    fontSize = 31.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 13.dp, bottom = 7.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Eyebrow(work.author)
                Text(work.title, fontFamily = FontFamily.Serif, color = Ink,
                    fontSize = 20.sp, lineHeight = 24.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(work.description, color = Muted, fontSize = 11.sp,
                    lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${lessons} STUDY UNITS", fontSize = 10.sp, color = Mint,
                        fontWeight = FontWeight.SemiBold, letterSpacing = .7.sp)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowForward, null, tint = Brass, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}
