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
import kotlinx.coroutines.*


// Importación de la anotación que permite el uso experimental de APIs en Kotlin
@OptIn(BetaOpenAI::class)
class Agent(private var token: String, private var assistantId: String) {
    // Declaración de variables para interactuar con la API de OpenAI y gestionar el asistente y los hilos
    private var openAI: OpenAI = OpenAI(token)
    private var assistant: Assistant? = null
    private var thread: Thread? = null


    // Define un manejador de excepciones para las corutinas
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        println("Error en corutina: ${exception.localizedMessage}")
    }
    private val coroutineScope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)

    // Método para inicializar el agente
    suspend fun initialize() {
        initializeOpenAI()
        initializeAssistantAndThread()
    }


    // Método para inicializar el cliente de OpenAI con el token proporcionado
    private fun initializeOpenAI() {
        openAI = OpenAI(token)
    }

    // Método para inicializar el asistente y el hilo de manera asíncrona

    private suspend fun initializeAssistantAndThread() = coroutineScope {
        // Ejecución paralela con async-await
        val assistantDeferred = async { createAssistant(assistantId) }
        val threadDeferred = async { createThread() }
        // Espera a que ambas operaciones completen
        assistant = assistantDeferred.await()
        thread = threadDeferred.await()
    }


    // Método para crear un asistente con el ID proporcionado

    private suspend fun createAssistant(assistantId: String): Assistant? {
        return try {
            openAI.assistant(id = AssistantId(assistantId))
        } catch (e: Exception) {
            println("Error al crear el asistente: ${e.message}")
            null // Retorna null en caso de error
        }
    }


    // Método para crear un nuevo hilo de conversación

    private suspend fun createThread(): Thread? {
        return try {
            openAI.thread()
        } catch (e: Exception) {
            println("Error al crear el hilo: ${e.message}")
            null // Retorna null en caso de error
        }
    }

    // Método para enviar un mensaje al asistente y recibir su respuesta
    private suspend fun chat(message: String): String {
        val currentThread = thread
        val currentAssistant = assistant
        if (currentThread != null && currentAssistant != null) {
            return try {
                openAI.message(
                    threadId = currentThread.id,
                    request = MessageRequest(role = Role.User, content = message)
                )
                var runStatus: Status
                val run = openAI.createRun(
                    threadId = currentThread.id,
                    request = RunRequest(assistantId = currentAssistant.id)
                )

                // Bucle para esperar hasta que la ejecución del mensaje esté completa
                do {
                    delay(1000) // Espera 1 segundo antes de consultar el estado
                    val runTest = openAI.getRun(currentThread.id, run.id)
                    runStatus = runTest.status
                } while (runStatus != Status.Completed)

                // Obtiene los mensajes del hilo y devuelve el último mensaje del asistente
                val messages = openAI.messages(currentThread.id)
                val lastAssistantMessage = messages.find { it.role == Role.Assistant }
                lastAssistantMessage?.content?.firstOrNull()?.let { content ->
                    if (content is MessageContent.Text) content.text.value else "Error: La respuesta del asistente no es texto."
                } ?: "Error: No se encontró respuesta del asistente."
            } catch (e: Exception) {
                "Se produjo un error al procesar su solicitud: ${e.message}"
            }
        } else {
            return "Error: El asistente o el hilo no está inicializado correctamente."
        }
    }

    // Método para actualizar el token de la API de OpenAI
    private fun updateApiToken() {
        token = readValidLine("Ingrese la nueva OPENAI_API_KEY: ")
        initializeOpenAI()
        println("OPENAI_API_KEY actualizado correctamente.")
    }

    // Método para actualizar el ID del asistente
    private fun updateAssistantId() {
        assistantId = readValidLine("Ingrese el nuevo OPENAI_ASSISTANT_ID: ")
        println("OPENAI_ASSISTANT_ID actualizado correctamente. Por favor, reinicie el asistente.")
    }

    // Método para interactuar con el asistente mediante la consola
    private suspend fun chatWithAssistant() {
        while (true) {
            val message = readValidLine(
                "Escribe tu mensaje (o escribe 'salir' para volver al menú principal): ",
                "salir"
            )
            if (message.equals("salir", ignoreCase = true)) break
            val response = chat(message)
            println("Respuesta: $response")
        }
    }

    // Método para ejecutar un bucle de comandos, permitiendo al usuario interactuar con el agente
    suspend fun runCommandLoop() {
        while (true) {
            println("Opciones: \n1. Actualizar OPENAI_ASSISTANT_ID \n2. Escribir mensaje \n3. Actualizar OPENAI_API_KEY \n4. Salir")
            when (readlnOrNull()) {
                "1" -> updateAssistantId()
                "2" -> chatWithAssistant()
                "3" -> updateApiToken()
                "4" -> exitApplication()
                else -> println("Opción no reconocida, intenta de nuevo.")
            }
        }
    }

    private fun exitApplication() {
        println("Cerrando aplicación...")
        coroutineScope.cancel("Preparando para cerrar la aplicación")
        // Espera a que las corutinas finalicen después de la cancelación, si es necesario.
        runBlocking {
            coroutineScope.coroutineContext[Job]?.join()
        }
        kotlin.system.exitProcess(0)
    }


}


// Función para leer una línea válida de la entrada del usuario, con opción de comando de salida
fun readValidLine(prompt: String, exitCommand: String = ""): String {
    var line: String?
    do {
        println(prompt)
        line = readlnOrNull()
        if (line.isNullOrEmpty()) {
            println("La entrada no puede estar vacía.")
        } else if (line.equals(exitCommand, ignoreCase = true)) {
            break
        }
    } while (line.isNullOrEmpty() || line.equals(exitCommand, ignoreCase = true))
    return line ?: exitCommand
}

// Objeto para configurar y obtener variables de entorno o entradas del usuario
object Configuration {
    fun getEnvVariableOrUserInput(envVarName: String, promptMessage: String): String {
        var result = System.getenv(envVarName)
        while (result.isNullOrEmpty()) {
            println("$promptMessage: ")
            result = readln().trim()
            if (result.isEmpty()) {
                println("$envVarName no puede estar vacío.")
            }
        }
        return result
    }
}

// Función principal para ejecutar el agente
suspend fun main() {
    val token = Configuration.getEnvVariableOrUserInput(
        "OPENAI_API_KEY",
        "OPENAI_API_KEY no está configurado. Por favor, ingresa una nueva OPENAI_API_KEY"
    )
    val assistantId = Configuration.getEnvVariableOrUserInput(
        "OPENAI_ASSISTANT_ID",
        "OPENAI_ASSISTANT_ID no está configurado. Por favor, ingresa OPENAI_ASSISTANT_ID"
    )

    val agent = Agent(token, assistantId)
    agent.initialize() // Esperar a que esta inicialización se complete
    agent.runCommandLoop()
}
