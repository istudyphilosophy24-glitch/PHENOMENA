package com.phenomena.study.data

import android.content.Context
import com.phenomena.study.model.*
import org.json.JSONArray
import org.json.JSONObject

private fun JSONObject.str(key: String) = optString(key, "")
private fun JSONObject.strings(key: String): List<String> = optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
private inline fun <T> JSONArray.mapItems(create: (JSONObject) -> T) = (0 until length()).map { create(getJSONObject(it)) }

class Catalog(context: Context) {
    private fun asset(path: String): JSONObject = JSONObject(context.assets.open(path).bufferedReader().use { it.readText() })
    private val root = asset("library.json")
    private val termsRoot = asset("lexicon.json")
    val works = root.getJSONArray("works").mapItems { o ->
        Work(o.str("id"), o.str("author"), o.str("title"), o.str("year"),
            android.graphics.Color.parseColor(o.str("color")), o.str("description"), o.str("shortTitle"))
    }
    val lessons = root.getJSONArray("lessons").mapItems { o ->
        Lesson(o.str("id"), o.str("workId"), o.str("section"), o.str("title"), o.optInt("minutes", 15),
            o.strings("tags"), o.str("synopsis"), o.str("exposition"), o.str("advanced"),
            o.getJSONArray("scaffold").mapItems { Scaffold(it.str("title"), it.str("body")) },
            o.strings("vocabulary"),
            o.getJSONArray("argument").mapItems { ArgumentStep(it.str("title"), it.str("detail"), it.str("kind")) },
            o.str("example"), o.str("critique"), o.str("comparison"), o.str("seminar"), o.str("debate"),
            o.getJSONArray("beats").mapItems { Beat(it.str("title"), it.str("content"), it.str("kind")) },
            o.str("sourceNote"))
    }
    val lexicon = termsRoot.getJSONArray("terms").mapItems { o ->
        Term(o.str("id"), o.str("title"), o.str("original"), o.str("language"),
            o.str("definition"), o.str("nuance"), o.strings("relations"), o.strings("works"))
    }
    fun work(id: String) = works.firstOrNull { it.id == id }
    fun lesson(id: String) = lessons.firstOrNull { it.id == id }
    fun term(id: String) = lexicon.firstOrNull { it.id == id }
}
