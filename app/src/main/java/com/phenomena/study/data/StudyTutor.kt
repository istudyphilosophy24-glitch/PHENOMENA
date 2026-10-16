package com.phenomena.study.data

import com.phenomena.study.model.Lesson
import com.phenomena.study.model.Term
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Deterministic, inspectable and offline: retrieves relevant commentary and identifies its source layer. */
object OfflineCoach {
    fun answer(question: String, lesson: Lesson, lexicon: List<Term>): String {
        val query = question.lowercase().split(Regex("[^\\p{L}]+"))
            .filter { it.length >= 4 && it !in setOf("what", "does", "mean", "explain", "heidegger", "husserl", "about", "would", "from", "that") }
            .toSet()
        val terms = lexicon.filter { term ->
            val hay = (term.title + " " + term.original + " " + term.definition).lowercase()
            query.any { it in hay } || query.any { it in term.id }
        }.take(2)
        val candidates = listOf(
            "Textual synopsis" to lesson.synopsis,
            "Scholarly exposition" to lesson.exposition,
            "Advanced analysis" to lesson.advanced,
            "Critical commentary" to lesson.critique,
            "Comparative philosophy" to lesson.comparison,
            "Phenomenological example" to lesson.example
        )
        val result = candidates.maxByOrNull { (_, section) -> query.sumOf { token -> if (token in section.lowercase()) 1 else 0 } } ?: candidates.first()
        return buildString {
            append("SOURCE-GROUNDED STUDY RESPONSE\n\n")
            if (terms.isNotEmpty()) {
                append("Relevant terms:\n")
                terms.forEach { append("• ${it.title} (${it.original}): ${it.definition}\n") }
                append("\n")
            }
            append("${result.first} · ${lesson.section}\n\n${result.second}\n\n")
            append("This offline coach retrieves relevant curated commentary; it does not generate a new argument or independently verify an interpretation. For a contextual answer, enable the optional online tutor below.")
        }
    }
}

/** Explicit opt-in API call. Token stays in UI memory, never saved to storage or embedded in a build. */
object OnlineTutor {
    suspend fun ask(question: String, lesson: Lesson, key: String, model: String): String = withContext(Dispatchers.IO) {
        require(key.isNotBlank()) { "Enter your own API key first" }
        require(question.isNotBlank()) { "Ask a question first" }
        val conn = URL("https://api.openai.com/v1/responses").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"; conn.connectTimeout = 25000; conn.readTimeout = 60000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $key")
            val context = """You are a rigorous specialist in phenomenology. Discuss the user's question by grounding claims in the ORIGINAL, curated lesson notes below. Clearly distinguish direct statements in the target work from interpretations and disputed scholarship. Do not invent quotations or page citations. If the commentary does not justify a definite answer, explain the uncertainty.\nWork segment: ${lesson.section}: ${lesson.title}\nSynopsis: ${lesson.synopsis}\nExposition: ${lesson.exposition}\nAdvanced: ${lesson.advanced}\nObjections: ${lesson.critique}\nComparison: ${lesson.comparison}"""
            val request = JSONObject().apply {
                put("model", model.ifBlank { "gpt-5-mini" })
                put("instructions", context)
                put("input", question.take(4000))
                put("max_output_tokens", 1100)
            }
            conn.outputStream.use { it.write(request.toString().toByteArray(Charsets.UTF_8)) }
            val response = (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (conn.responseCode !in 200..299) throw IllegalStateException("API error ${conn.responseCode}: ${response.take(220)}")
            val root = JSONObject(response)
            val output = root.optJSONArray("output") ?: JSONArray()
            val text = buildString {
                for (i in 0 until output.length()) {
                    val pieces = output.optJSONObject(i)?.optJSONArray("content") ?: continue
                    for (j in 0 until pieces.length()) {
                        val part = pieces.optJSONObject(j) ?: continue
                        if (part.optString("type") == "output_text") append(part.optString("text"))
                    }
                }
            }
            if (text.isBlank()) "The model returned no displayable text. Please try again." else text
        } finally { conn.disconnect() }
    }
}
