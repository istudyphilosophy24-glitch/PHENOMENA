package com.phenomena.study.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phenomena.study.data.*
import com.phenomena.study.model.Lesson
import com.phenomena.study.model.Term
import kotlinx.coroutines.launch

@Composable
fun TutorScreen(lesson: Lesson, lexicon: List<Term>) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var useOnline by remember { mutableStateOf(false) }
    var key by remember { mutableStateOf("") } // never persisted
    var model by remember { mutableStateOf("gpt-5-mini") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth().heightIn(max = 540.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${lesson.section} · ${lesson.title}", fontSize = 12.sp, color = Muted)
        Body("Ask about this section. Offline answers retrieve editorial commentary. Optional online mode uses your own API key and sends your question with the curated lesson notes.", color = Muted)
        OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("Your philosophical question") },
            placeholder = { Text("Why is Dasein ontically near but ontologically distant?") }, minLines = 2, maxLines = 5,
            modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = useOnline, onCheckedChange = { useOnline = it })
            Text("Use online generative tutor (opt-in)", fontSize = 12.sp)
        }
        if (useOnline) {
            OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("Personal OpenAI API key") },
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("API model identifier") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text("API usage may incur provider charges. Credentials are kept only in transient UI memory, not saved in the database.",
                fontSize = 11.sp, color = Muted)
        }
        PrimaryAction(if (busy) "Thinking…" else if (useOnline) "Ask online tutor" else "Find grounded answer",
            Modifier.fillMaxWidth()) {
            if (!busy && question.isNotBlank()) scope.launch {
                busy = true
                answer = try {
                    if (useOnline) OnlineTutor.ask(question, lesson, key.trim(), model.trim())
                    else OfflineCoach.answer(question, lesson, lexicon)
                } catch (e: Exception) { "Could not answer: ${e.message}" }
                busy = false
            }
        }
        if (answer.isNotBlank()) {
            Rule()
            Eyebrow(if (useOnline) "AI INTERPRETATION" else "EDITORIAL SOURCE RETRIEVAL")
            Body(answer)
        }
        Text("Interpretations may be disputed. Verify substantive claims against an authorized source text and scholarship.",
            fontSize = 11.sp, color = Muted)
    }
}
