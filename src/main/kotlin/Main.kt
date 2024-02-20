package com.josejordan

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.assistant.Assistant
import com.aallam.openai.api.assistant.AssistantId
import com.aallam.openai.api.core.Role
import com.aallam.openai.api.core.Status
import com.aallam.openai.api.message.MessageContent
import com.aallam.openai.api.message.MessageRequest
import com.aallam.openai.api.run.RunRequest
import com.aallam.openai.api.thread.Thread
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

@OptIn(BetaOpenAI::class)
class Agent(var token: String) {
    private var openAI: OpenAI = OpenAI(token)
    var assistantId: String? = null
    private lateinit var assistant: Assistant
    private lateinit var thread: Thread

    init {
        // Intenta cargar OPENAI_ASSISTANT_ID al inicio
        assistantId = System.getenv("OPENAI_ASSISTANT_ID")
        if (assistantId == null) {
            println("Advertencia: OPENAI_ASSISTANT_ID no está configurado. Se requerirá configuración adicional.")
        }
    }

    suspend fun initialize() {
        if (assistantId == null) {
            throw IllegalStateException("OPENAI_ASSISTANT_ID no está configurado.")
        }
        createAssistant(assistantId!!)
        createThread()
    }

    private suspend fun createAssistant(assistantId: String) {
        assistant = openAI.assistant(id = AssistantId(assistantId)) ?: throw IllegalStateException("No se pudo obtener el asistente con ID: $assistantId")
    }

    private suspend fun createThread() {
        thread = openAI.thread()
    }

    suspend fun chat(message: String): String {
        try {
            openAI.message(
                threadId = thread.id,
                request = MessageRequest(
                    role = Role.User,
                    content = message
                )
            )

            val run = openAI.createRun(
                threadId = thread.id,
                request = RunRequest(assistantId = assistant.id)
            )

            do {
                delay(1_000)
                val runTest = openAI.getRun(thread.id, run.id)
            } while (runTest.status != Status.Completed)

            val messages = openAI.messages(thread.id)
            val lastAssistantMessage = messages.find { it.role == Role.Assistant }
            return lastAssistantMessage?.content?.firstOrNull()?.let { content ->
                if (content is MessageContent.Text) content.text.value else "<Assistant Error>"
            } ?: "<Assistant Error>"
        } catch (e: Exception) {
            return "Se produjo un error al procesar su solicitud: ${e.message}"
        }
    }

    fun updateToken(newToken: String) {
        this.token = newToken
        this.openAI = OpenAI(token)
    }

    fun updateAssistantId(newAssistantId: String) {
        this.assistantId = newAssistantId
        println("OPENAI_ASSISTANT_ID actualizado correctamente.")
    }
}

fun main() = runBlocking {
    println("Verificando configuración del OPENAI_API_KEY...")
    var token = System.getenv("OPENAI_API_KEY")
    if (token.isNullOrEmpty()) {
        println("OPENAI_API_KEY no está configurado. Por favor, ingresa una nueva OPENAI_API_KEY: ")
        token = readlnOrNull() ?: throw IllegalArgumentException("OPENAI_API_KEY es necesario.")
    }

    println("Verificando configuración del OPENAI_ASSISTANT_ID...")
    var assistantId = System.getenv("OPENAI_ASSISTANT_ID")
    if (assistantId.isNullOrEmpty()) {
        println("OPENAI_ASSISTANT_ID no está configurado. Por favor, ingresa OPENAI_ASSISTANT_ID: ")
        assistantId = readlnOrNull() ?: throw IllegalArgumentException("OPENAI_ASSISTANT_ID es necesario.")
    }

    val agent = Agent(token).apply {
        this.assistantId = assistantId
    }

    println("Inicializando asistente...")
    runBlocking { agent.initialize() }


    while (true) {
        println("Opciones: \n1. Actualizar OPENAI_ASSISTANT_ID \n2. Escribir mensaje \n3. Actualizar OPENAI_API_KEY \n4. Salir")
        when (readlnOrNull()) {
            "1" -> {
                println("Ingrese el nuevo OPENAI_ASSISTANT_ID: ")
                val newAssistantId = readlnOrNull() ?: throw IllegalArgumentException("OPENAI_ASSISTANT_ID es necesario.")
                agent.updateAssistantId(newAssistantId)
                runBlocking { agent.initialize() }

            }
            "2" -> {
                while (true) {
                    println("Escribe tu mensaje (o escribe 'salir' para volver al menú principal): ")
                    val message = readlnOrNull()
                    if (message.equals("salir", ignoreCase = true)) break
                    val response = agent.chat(message ?: continue)
                    println("Respuesta: $response")
                }
            }
            "3" -> {
                println("¿Deseas actualizar o introducir una nueva OPENAI_API_KEY? (yes/no): ")
                if (readlnOrNull()?.lowercase() == "yes") {
                    println("Ingrese la nueva OPENAI_API_KEY: ")
                    val newToken = readlnOrNull() ?: throw IllegalArgumentException("OPENAI_API_KEY es necesario.")
                    agent.updateToken(newToken)
                    println("OPENAI_API_KEY actualizado correctamente.")
                }
            }
            "4", "salir" -> break
            else -> println("Opción no reconocida, intenta de nuevo.")
        }
    }
}
