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
class Agent(token: String) {
    private var openAI: OpenAI = OpenAI(token)
    var assistantId: String? = System.getenv("OPENAI_ASSISTANT_ID")
    private var assistant: Assistant? = null
    private var thread: Thread? = null

    suspend fun initialize() {
        assistantId ?: throw IllegalStateException("OPENAI_ASSISTANT_ID no está configurado.")
        createAssistant(assistantId!!)
        createThread()
    }

    private suspend fun createAssistant(assistantId: String) {
        assistant = openAI.assistant(id = AssistantId(assistantId))
    }

    private suspend fun createThread() {
        thread = openAI.thread()
    }

    suspend fun chat(message: String): String = try {
        openAI.message(
            threadId = thread!!.id,
            request = MessageRequest(role = Role.User, content = message)
        )

        var runStatus: Status
        val run = openAI.createRun(threadId = thread!!.id, request = RunRequest(assistantId = assistant!!.id))

        do {
            delay(1_000)
            val runTest = openAI.getRun(thread!!.id, run.id)
            runStatus = runTest.status
        } while (runStatus != Status.Completed)

        val messages = openAI.messages(thread!!.id)
        val lastAssistantMessage = messages.find { it.role == Role.Assistant }
        lastAssistantMessage?.content?.firstOrNull()?.let { content ->
            if (content is MessageContent.Text) content.text.value else "<Assistant Error>"
        } ?: "<Assistant Error>"
    } catch (e: Exception) {
        "Se produjo un error al procesar su solicitud: ${e.message}"
    }


    fun updateAssistantId() = runBlocking {
        println("Ingrese el nuevo OPENAI_ASSISTANT_ID: ")
        val newAssistantId = readlnOrNull()
        if (!newAssistantId.isNullOrEmpty()) {
            this@Agent.assistantId = newAssistantId
            this@Agent.initialize()
            println("Asistente actualizado con éxito.")
        } else {
            println("El OPENAI_ASSISTANT_ID no puede estar vacío.")
        }
    }

    fun chatWithAssistant() = runBlocking {
        while (true) {
            println("Escribe tu mensaje (o escribe 'salir' para volver al menú principal): ")
            val message = readlnOrNull()
            if (message.equals("salir", ignoreCase = true)) break
            val response = chat(message ?: continue)
            println("Respuesta: $response")
        }
    }

    fun updateApiToken() = runBlocking {
        println("Ingrese la nueva OPENAI_API_KEY: ")
        val newToken = readlnOrNull()
        if (!newToken.isNullOrEmpty()) {
            openAI = OpenAI(newToken)
            println("OPENAI_API_KEY actualizado correctamente.")
        } else {
            println("La OPENAI_API_KEY no puede estar vacía.")
        }
    }


}

fun main() = runBlocking {
    println("Inicializando configuración y asistente...")

    val token = System.getenv("OPENAI_API_KEY") ?: readlnOrNull() ?: throw IllegalArgumentException("OPENAI_API_KEY es necesario.")
    val agent = Agent(token)

    agent.assistantId = System.getenv("OPENAI_ASSISTANT_ID") ?: readlnOrNull() ?: throw IllegalArgumentException("OPENAI_ASSISTANT_ID es necesario.")
    agent.initialize()

    commandLoop@ while (true) {
        println("Opciones: \n1. Actualizar OPENAI_ASSISTANT_ID \n2. Escribir mensaje \n3. Actualizar OPENAI_API_KEY \n4. Salir")
        when (readlnOrNull()) {
            "1" -> agent.updateAssistantId()
            "2" -> agent.chatWithAssistant()
            "3" -> agent.updateApiToken()
            "4", "salir" -> break@commandLoop
            else -> println("Opción no reconocida, intenta de nuevo.")
        }
    }
}
