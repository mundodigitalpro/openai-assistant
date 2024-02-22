async function sendMessage() {
    const inputElement = document.getElementById('messageInput');
    const message = inputElement.value;
    const chatContainer = document.getElementById('chatContainer');

    // Añadir el mensaje enviado al contenedor
    const userMessageElement = document.createElement('div');
    userMessageElement.classList.add('chat-message', 'sent');
    userMessageElement.innerHTML = `<div class="message-text">${message}</div>`;
    chatContainer.appendChild(userMessageElement);

    try {
        const response = await fetch('/chat', {
            method: 'POST',
            body: message, // Envía el mensaje como cuerpo de la solicitud
        });

        if (!response.ok) {
            throw new Error(`Error HTTP: ${response.status}`);
        }

        const responseData = await response.text();

        // Añadir la respuesta recibida al contenedor
        const responseMessageElement = document.createElement('div');
        responseMessageElement.classList.add('chat-message', 'received');
        responseMessageElement.innerHTML = `<div class="message-text">${responseData}</div>`;
        chatContainer.appendChild(responseMessageElement);

        // Limpiar el input y mantener el scroll al final
        inputElement.value = '';
        chatContainer.scrollTop = chatContainer.scrollHeight;
    } catch (error) {
        console.error('Error al enviar mensaje:', error);
    }
}
