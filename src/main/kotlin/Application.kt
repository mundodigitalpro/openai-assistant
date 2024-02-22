package com.josejordan

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import kotlinx.coroutines.runBlocking

fun main() {
    val token = System.getenv("OPENAI_API_KEY") ?: throw IllegalStateException("OPENAI_API_KEY no está configurado.")
    val assistantId = System.getenv("OPENAI_ASSISTANT_ID") ?: throw IllegalStateException("OPENAI_ASSISTANT_ID no está configurado.")

    embeddedServer(Netty, port = 8080) {
        routing {
            post("/chat") {
                val requestBody = call.receiveText()
                val agent = Agent(token, assistantId) // Usa las variables de entorno aquí
                val chatResponse = runBlocking {
                    agent.initialize()
                    agent.chat(requestBody)
                }
                call.respondText(chatResponse)
            }

            static("/") {
                resources("static")
                defaultResource("index.html", "static")
            }
        }
    }.start(wait = true)
}