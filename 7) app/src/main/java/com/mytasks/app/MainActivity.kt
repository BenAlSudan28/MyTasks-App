package com.mytasks.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Task(val id: Long, val title: String, val done: Boolean, val color: Int)

val Palette = listOf(
    Color(0xFF7C4DFF), Color(0xFF00B8D4), Color(0xFFFF4081),
    Color(0xFFFF9100), Color(0xFF00C853), Color(0xFF2979FF)
)
val BgColor = Color(0xFFF6F4FF)

fun loadTasks(p: SharedPreferences): List<Task> {
    val raw = p.getString("data", "") ?: ""
    return raw.split("\n").filter { it.isNotBlank() }.mapNotNull {
        try {
            val s = it.split("|", limit = 4)
            Task(s[0].toLong(), s[3], s[1] == "true", s[2].toInt())
        } catch (e: Exception) {
            null
        }
    }
}

fun saveTasks(p: SharedPreferences, list: List<Task>) {
    p.edit().putString(
        "data",
        list.joinToString("\n") { "${it.id}|${it.done}|${it.color}|${it.title}" }
    ).apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("tasks", Context.MODE_PRIVATE)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = lightColorScheme(primary = Palette[0])) {
                    TasksScreen(prefs)
                }
            }
        }
    }
}

@Composable
fun TasksScreen(prefs: SharedPreferences) {
    val tasks = remember { loadTasks(prefs).toMutableStateList() }
    var showDialog by remember { mutableStateOf(false) }

    val doneCount = tasks.count { it.done }
    val progress = if (tasks.isEmpty()) 0f else doneCount.toFloat() / tasks.size

    Scaffold(
        containerColor = BgColor,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Palette[0],
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("مهمة جديدة", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            Header(doneCount, tasks.size, progress)

            if (tasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد مهام بعد ✨\nاضغط على «مهمة جديدة» للبدء",
                        fontSize = 16.sp, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task,
                            onToggle = {
                                val i = tasks.indexOfFirst { t -> t.id == task.id }
                                if (i >= 0) tasks[i] = task.copy(done = !task.done)
                                saveTasks(prefs, tasks)
                            },
                            onDelete = {
                                tasks.removeAll { t -> t.id == task.id }
                                saveTasks(prefs, tasks)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddDialog(
            onDismiss = { showDialog = false },
            onAdd = { title, color ->
                tasks.add(0, Task(System.currentTimeMillis(), title, false, color))
                saveTasks(prefs, tasks)
                showDialog = false
            }
        )
    }
}

@Composable
fun Header(done: Int, total: Int, progress: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF7C4DFF), Color(0xFF448AFF), Color(0xFFFF4081))))
            .padding(24.dp, 56.dp, 24.dp, 28.dp)
    ) {
        Column {
            Text("مهامي ✅", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (total == 0) "ابدأ يومك بإضافة مهمة" else "أنجزت $done من $total مهام",
                color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp
            )
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun TaskCard(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    val c = Palette[task.color % Palette.size]
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .clickable { onToggle() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.width(6.dp).height(40.dp).clip(CircleShape).background(c)
            )
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (task.done) c else Color.Transparent)
                    .border(2.dp, c, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (task.done) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                task.title,
                modifier = Modifier.weight(1f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = if (task.done) Color.Gray else Color(0xFF222222),
                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFB0B0B0))
            }
        }
    }
}

@Composable
fun AddDialog(onDismiss: () -> Unit, onAdd: (String, Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    var selected by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("مهمة جديدة", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.replace("|", " ").replace("\n", " ") },
                    placeholder = { Text("ماذا تريد أن تنجز؟") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Palette.forEachIndexed { i, col ->
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(if (i == selected) 3.dp else 0.dp, Color.DarkGray, CircleShape)
                                .clickable { selected = i }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onAdd(text.trim(), selected) },
                shape = RoundedCornerShape(14.dp)
            ) { Text("إضافة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
