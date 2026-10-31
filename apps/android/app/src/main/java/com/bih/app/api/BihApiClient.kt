package com.bih.app.api

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class BihApiClient(private val baseUrl: String) {
    fun chat(message: String): String {
        val body = JSONObject().put("message", message).toString()
        val json = postJson("$baseUrl/chat", body)
        return json.optString("reply", "")
    }

    fun analyzePaperwork(text: String): PaperworkResult {
        val body = JSONObject().put("text", text).toString()
        val json = postJson("$baseUrl/paperwork/analyze", body)
        return PaperworkResult(
            plainSummary = json.optString("plain_summary", ""),
            keyActions = json.optJSONArray("key_actions").toList(),
            deadlines = json.optJSONArray("deadlines").toList(),
            questionsToAsk = json.optJSONArray("questions_to_ask").toList(),
            safetyNote = json.optString("safety_note", ""),
        )
    }

    private fun postJson(endpoint: String, body: String): JSONObject {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toByteArray()) }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw IllegalStateException("API error $status: $text")
            }
            JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }
}

data class PaperworkResult(
    val plainSummary: String,
    val keyActions: List<String>,
    val deadlines: List<String>,
    val questionsToAsk: List<String>,
    val safetyNote: String,
)

private fun JSONArray?.toList(): List<String> {
    if (this == null) return emptyList()
    val out = mutableListOf<String>()
    for (i in 0 until length()) {
        out.add(optString(i))
    }
    return out
}
