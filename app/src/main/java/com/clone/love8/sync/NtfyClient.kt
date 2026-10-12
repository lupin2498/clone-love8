package com.clone.love8.sync

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Client tối giản cho máy chủ ntfy (https://ntfy.sh hoặc máy chủ tự host). */
object NtfyClient {
    data class Msg(val id: String, val time: Long, val message: String)

    private fun base(server: String) = server.trim().trimEnd('/')

    fun publish(server: String, topic: String, body: String): Boolean {
        val conn = URL("${base(server)}/$topic").openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        } finally {
            conn.disconnect()
        }
    }

    /** Lấy các tin nhắn đã lưu trên máy chủ kể từ [since] (id tin nhắn, "all" hoặc khoảng thời gian). */
    fun poll(server: String, topic: String, since: String): List<Msg>? {
        val url = URL("${base(server)}/$topic/json?poll=1&since=${URLEncoder.encode(since, "UTF-8")}")
        val conn = url.openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            if (conn.responseCode !in 200..299) {
                null
            } else {
                conn.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.mapNotNull { parse(it) }.toList()
                }
            }
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(line: String): Msg? {
        if (line.isBlank()) return null
        return try {
            val o = JSONObject(line)
            if (o.optString("event") != "message") {
                null
            } else {
                Msg(o.getString("id"), o.optLong("time"), o.optString("message"))
            }
        } catch (e: Exception) {
            null
        }
    }

    fun ping(server: String): Boolean {
        val conn = URL("${base(server)}/v1/health").openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        } finally {
            conn.disconnect()
        }
    }
}
