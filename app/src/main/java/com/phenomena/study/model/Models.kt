package com.phenomena.study.model

data class Work(
    val id: String, val author: String, val title: String, val year: String,
    val color: Int, val description: String, val shortTitle: String
)

data class Scaffold(val title: String, val body: String)
data class ArgumentStep(val title: String, val detail: String, val kind: String)
data class Beat(val title: String, val content: String, val kind: String)
data class Term(
    val id: String, val title: String, val original: String, val language: String,
    val definition: String, val nuance: String, val relations: List<String>, val works: List<String>
)
data class Lesson(
    val id: String, val workId: String, val section: String, val title: String,
    val minutes: Int, val tags: List<String>, val synopsis: String, val exposition: String,
    val advanced: String, val scaffold: List<Scaffold>, val vocabulary: List<String>,
    val argument: List<ArgumentStep>, val example: String, val critique: String,
    val comparison: String, val seminar: String, val debate: String,
    val beats: List<Beat>, val sourceNote: String
)
