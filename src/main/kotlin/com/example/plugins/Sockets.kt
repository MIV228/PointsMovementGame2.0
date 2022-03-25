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
import kotlinx.coroutines.yield
import java.util.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

var lastClear1 = System.currentTimeMillis()

var newPosition = 0.0 to 0.0
var realPosition = 0.0 to 0.0

var speed = 0.4  //0.05f

fun Application.configureSockets() {

    val players = Collections.synchronizedMap<DefaultWebSocketSession, Pair<Double,Double>>(mutableMapOf())
//    var walls = mutableMapOf<Pair<Pair<Int, Int>, Pair<Int, Int>>, Int>()

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }


    routing {
        webSocket("/") { // websocketSession
            players[this] = 0.0 to 0.0
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {

                        val position = frame.readText().split(",")
                        val x = position.first().toInt()
                        val y = position.last().toInt()
                        realPosition = players[this]!!
                        newPosition = x.toDouble() to y.toDouble()

                        yield()
                        val a = System.currentTimeMillis() - lastClear1
                        lastClear1 = System.currentTimeMillis()
                        if (abs(x - newPosition.first) > 3.0 ||
                            abs(y - newPosition.second) > 3.0
                        ) {
                            val xLength = newPosition.first - x
                            val yLength = newPosition.second - y
                            val C = sqrt(xLength.pow(2) + yLength.pow(2))
                            val cos = yLength / C
                            val sin = xLength / C
                            val dx =  a * speed * sin
                            val dy =  a * speed * cos
                            realPosition = x.toDouble() + dx to y.toDouble() + dy
                            updateCoord(
                                realPosition
                            )
                            players[this] = realPosition
                        }

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

fun updateCoord(newPosition : Pair<Double,Double>){
    realPosition = newPosition
}