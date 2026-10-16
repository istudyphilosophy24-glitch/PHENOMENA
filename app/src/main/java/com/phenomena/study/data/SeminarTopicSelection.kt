package com.phenomena.study.data

/** Pure selection policy so topic matching can be regression-tested without Android. */
object SeminarTopicSelection {
    fun initialIds(workId: String?, modules: List<Pair<String,String>>, limit: Int = 24): List<String> {
        if (workId.isNullOrBlank() || limit <= 0) return emptyList()
        return modules.filter { it.second == workId }
            .map { it.first }.distinct().take(limit)
    }
}
