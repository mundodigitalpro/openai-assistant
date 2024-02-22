package com.josejordan

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import kotlinx.coroutines.runBlocking


// Variable global para mantener la instancia del agente
val agent by lazy {
    val token = System.getenv("OPENAI_API_KEY") ?: throw IllegalStateException("OPENAI_API_KEY no está configurado.")
    val assistantId =
        System.getenv("OPENAI_ASSISTANT_ID") ?: throw IllegalStateException("OPENAI_ASSISTANT_ID no está configurado.")
    Agent(token, assistantId).apply {
        // Inicialización del agente y su hilo de conversación de forma asíncrona en el contexto de bloqueo
        runBlocking { this@apply.initialize() }
    }
}

fun main() {
    embeddedServer(Netty, port = 8080) {
        routing {
            post("/chat") {
                val requestBody = call.receiveText()
                // Utiliza la instancia global del agente
                val chatResponse = runBlocking {
                    agent.chat(requestBody)
                }
                call.respondText(chatResponse)
            }

            staticResources("/", "static") {
                default("index.html")

            }
        }
    }.start(wait = true)
}
