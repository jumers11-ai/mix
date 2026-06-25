package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiAssistant {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Escape special characters in text to prevent breaking the raw JSON body structure.
     */
    private fun escapeJson(text: String): String {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Parse the response JSON using simple, robust substring matching.
     * This avoids heavy reflection / annotation processing of third-party JSON libraries.
     */
    private fun extractTextFromGeminiJson(json: String): String {
        val searchStr = "\"text\""
        val index = json.indexOf(searchStr)
        if (index == -1) {
            // Check if there was an API error block
            if (json.contains("\"error\"")) {
                val messageIndex = json.indexOf("\"message\"")
                if (messageIndex != -1) {
                    val firstQuote = json.indexOf("\"", messageIndex + "\"message\"".length + 1)
                    if (firstQuote != -1) {
                        val lastQuote = json.indexOf("\"", firstQuote + 1)
                        if (lastQuote != -1) {
                            return "Błąd API Gemini: " + json.substring(firstQuote + 1, lastQuote)
                        }
                    }
                }
            }
            return "Nie udało się odnaleźć tekstu w odpowiedzi API Gemini."
        }

        val colonIndex = json.indexOf(":", index)
        if (colonIndex == -1) return "Błąd struktury odpowiedzi."

        val firstQuote = json.indexOf("\"", colonIndex)
        if (firstQuote == -1) return "Błąd dopasowania cudzysłowu."

        val sb = StringBuilder()
        var i = firstQuote + 1
        while (i < json.length) {
            val c = json[i]
            if (c == '\\' && i + 1 < json.length) {
                val next = json[i + 1]
                when (next) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> sb.append('\r')
                    '\"' -> sb.append('\"')
                    '\\' -> sb.append('\\')
                    else -> sb.append(next)
                }
                i += 2
            } else if (c == '\"') {
                break
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString().trim()
    }

    /**
     * Ask Gemini for suggestions, passing current track library as context!
     */
    suspend fun chatWithAssistant(prompt: String, availableTracks: List<TrackEntity>): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Asystent AI: Skonfiguruj klucz GEMINI_API_KEY w panelu Secrets w AI Studio, aby aktywować inteligentny czat asystenta."
        }

        val trackContext = availableTracks.joinToString("\\n") {
            "- ID: ${it.id}, '${it.title}' autorstwa '${it.artist}' [BPM: ${it.bpm}, Key: ${it.camelotKey} / ${it.musicalKey}, Energia: ${it.energy}%, Gatunek: ${it.genre}]"
        }

        val systemPrompt = """
            Jesteś profesjonalnym, inteligentnym asystentem DJ-a o nazwie 'Smart DJ Assistant' zintegrowanym z aplikacją Mixgraph Solo.
            Pomagasz użytkownikowi tworzyć sety, dobierać utwory oraz miksować harmonicznie.
            Odpowiadaj profesjonalnie, rzeczowo i po polsku.
            
            Oto baza utworów, którymi dysponuje użytkownik w swojej lokalnej bibliotece:
            $trackContext
            
            Wskazówki miksowania harmonicznego (Camelot Wheel):
            - Możesz przejść do tej samej tonacji (np. 8A -> 8A).
            - Możesz przejść o +/- 1 krok (np. 8A -> 9A lub 8A -> 7A).
            - Możesz zmienić dur/moll w tym samym numerze (np. 8A <-> 8B).
            - Zmiana o +2 kroki (np. 8A -> 10A) podbija mocno energię setu.
            
            Jeśli użytkownik poprosi o zbudowanie setu (np. 'Zbuduj 90 minutowy set...'), wybierz najlepsze utwory z jego biblioteki, poukładaj je w idealnej kolejności (kompatybilność BPM i Camelot) i krótko opisz przejścia między nimi. Jeśli biblioteka jest za mała, możesz zaproponować dodanie znanych klasyków, ale priorytetyzuj utwory z powyższej listy!
        """.trimIndent()

        // Build raw JSON payload
        val requestJson = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": "${escapeJson(prompt)}"
                    }
                  ]
                }
              ],
              "systemInstruction": {
                "parts": [
                  {
                    "text": "${escapeJson(systemPrompt)}"
                  }
                ]
              },
              "generationConfig": {
                "temperature": 0.7
              }
            }
        """.trimIndent()

        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val requestBody = requestJson.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext "Błąd połączenia z API (Status ${response.code}): ${extractTextFromGeminiJson(responseBodyStr)}"
                }
                extractTextFromGeminiJson(responseBodyStr)
            }
        } catch (e: IOException) {
            "Błąd asystenta AI: ${e.message ?: "nieznany błąd sieci"}. Upewnij się, że masz połączenie z internetem i prawidłowy klucz API."
        }
    }
}
