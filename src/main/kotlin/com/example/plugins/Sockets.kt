package com.example.plugins

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.collections.*
import kotlinx.coroutines.launch
import java.util.*

fun Application.configureSockets() {

    val players = Collections.synchronizedMap<DefaultWebSocketSession, Pair<Int,Int>>(mutableMapOf())

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("/") { // websocketSession
            players[this] to (0 to 0)
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val position = frame.readText().split(",")
                        val x = position.first().toInt()
                        val y = position.last().toInt()
                        players[this] = x to y
                        val answer = players.values.joinToString(";") {
                            "${it.first},${it.second}"
                        }
                        players.forEach { t, u ->
                            launch {
                                t.send(Frame.Text(answer))
                            }
                        }
                    }
                }
            }
            players.remove(this)
        }
    }
}
