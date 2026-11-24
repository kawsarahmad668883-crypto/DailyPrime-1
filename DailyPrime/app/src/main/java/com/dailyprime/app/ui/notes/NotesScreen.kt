package com.dailyprime.app.ui.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailyprime.app.data.local.NoteEntity
import com.dailyprime.app.ui.theme.EmeraldPrimary
import com.dailyprime.app.ui.theme.GoldAccent
import com.dailyprime.app.ui.theme.parseHexColorOrDefault
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private val NOTE_CATEGORIES = listOf("All", "Personal", "Ideas", "Study", "Work", "Faith", "Health")
private val NOTE_COLORS = listOf(
    "#FFFFFF" to "White",
    "#ECFDF5" to "Emerald",
    "#FFFBEB" to "Gold",
    "#EFF6FF" to "Sky",
    "#FDF2F8" to "Rose",
    "#F5F3FF" to "Lavender"
)

@Composable
fun NotesScreen(
    notes: List<NoteEntity>,
    use24HourFormat: Boolean,
    onSaveNote: (NoteEntity) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var deleteConfirmId by remember { mutableStateOf<String?>(null) }

    val filteredNotes = remember(notes, searchQuery, selectedCategory) {
        val q = searchQuery.trim().lowercase()
        notes.filter { note ->
            val catOk = selectedCategory == "All" || note.category == selectedCategory
            val qOk = q.isEmpty() ||
                note.title.lowercase().contains(q) ||
                note.content.lowercase().contains(q) ||
                note.category.lowercase().contains(q)
            catOk && qOk
        }.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.updatedAt })
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val now = LocalDateTime.now()
                    val created = NoteEntity(
                        id = UUID.randomUUID().toString(),
                        title = "Untitled Note",
                        content = "",
                        category = if (selectedCategory == "All") "Personal" else selectedCategory,
                        colorHex = "#ECFDF5",
                        isPinned = false,
                        date = LocalDate.now().toString(),
                        time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                        createdAt = now.toString(),
                        updatedAt = now.toString()
                    )
                    onSaveNote(created)
                    editingNote = created
                },
                containerColor = EmeraldPrimary,
                contentColor = GoldAccent
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Note")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
        ) {
            item {
                Text(
                    text = "Notes & Journal",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Capture ideas, goals, and study notes with instant auto-save.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search notes by keyword or category...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NOTE_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }
            }

            if (filteredNotes.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No notes yet", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap + to create your first note.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredNotes, key = { it.id }) { note ->
                    val cardBg = parseHexColorOrDefault(note.colorHex, Color(0xFFFFFFFF))
                    val displayTime = formatNoteTime(note.time, use24HourFormat)

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, Color(0xFFD0DDD6)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingNote = note }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = note.title.ifBlank { "Untitled Note" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF0F1F1A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            onSaveNote(
                                                note.copy(
                                                    isPinned = !note.isPinned,
                                                    updatedAt = LocalDateTime.now().toString()
                                                )
                                            )
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                            contentDescription = "Pin Note",
                                            tint = if (note.isPinned) EmeraldPrimary else Color(0xFF4B6358),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = { deleteConfirmId = note.id }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Note",
                                            tint = Color(0xFFB91C1C),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (note.content.isNotBlank()) {
                                Text(
                                    text = note.content,
                                    fontSize = 13.sp,
                                    color = Color(0xFF2D433A),
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = note.category,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = "${note.date} · $displayTime",
                                    fontSize = 11.sp,
                                    color = Color(0xFF4B6358)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editingNote?.let { note ->
        var title by remember(note.id) { mutableStateOf(note.title) }
        var content by remember(note.id) { mutableStateOf(note.content) }
        var category by remember(note.id) { mutableStateOf(note.category) }
        var colorHex by remember(note.id) { mutableStateOf(note.colorHex) }
        var isPinned by remember(note.id) { mutableStateOf(note.isPinned) }

        fun autoSaveNow(
            newTitle: String = title,
            newContent: String = content,
            newCategory: String = category,
            newColorHex: String = colorHex,
            newPinned: Boolean = isPinned
        ) {
            onSaveNote(
                note.copy(
                    title = newTitle,
                    content = newContent,
                    category = newCategory,
                    colorHex = newColorHex,
                    isPinned = newPinned,
                    date = LocalDate.now().toString(),
                    time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                    updatedAt = LocalDateTime.now().toString()
                )
            )
        }

        AlertDialog(
            onDismissRequest = { editingNote = null },
            title = { Text("Edit Note (Auto-Saved)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            autoSaveNow(newTitle = it)
                        },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = {
                            content = it
                            autoSaveNow(newContent = it)
                        },
                        label = { Text("Description") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NOTE_CATEGORIES.drop(1).forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = {
                                    category = cat
                                    autoSaveNow(newCategory = cat)
                                },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NOTE_COLORS.forEach { (hex, _) ->
                            val swatch = parseHexColorOrDefault(hex, Color.White)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(
                                        width = if (colorHex == hex) 2.dp else 1.dp,
                                        color = if (colorHex == hex) EmeraldPrimary else Color.Gray.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        colorHex = hex
                                        autoSaveNow(newColorHex = hex)
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { editingNote = null }) {
                    Text("Done")
                }
            }
        )
    }

    deleteConfirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteConfirmId = null },
            title = { Text("Delete Note?") },
            text = { Text("This note will be permanently removed from your device.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteNote(id)
                        deleteConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatNoteTime(time24: String, use24Hour: Boolean): String {
    if (use24Hour) return time24
    return runCatching {
        LocalTime.parse(time24).format(DateTimeFormatter.ofPattern("hh:mm a"))
    }.getOrDefault(time24)
}
