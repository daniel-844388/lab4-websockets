@file:Suppress("NoWildcardImports")

package websockets

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.websocket.ClientEndpoint
import jakarta.websocket.ContainerProvider
import jakarta.websocket.OnMessage
import jakarta.websocket.Session
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.Locale
import java.util.Scanner

private val logger = KotlinLogging.logger {}

@SpringBootTest(webEnvironment = RANDOM_PORT)
class ElizaServerTest {
    @LocalServerPort
    private var port: Int = 0
    private val eliza = Eliza()

    @Test
    fun onOpen() {
        logger.info { "This is the test worker" }
        val latch = CountDownLatch(3)
        val list = mutableListOf<String>()

        val client = SimpleClient(list, latch)
        client.connect("ws://localhost:$port/eliza")
        latch.await()
        assertEquals(3, list.size)
        assertEquals("The doctor is in.", list[0])
    }

    //@Disabled // Remove this line when you implement onChat
    @Test
    fun onChat() {
        logger.info { "Test thread" }
        val latch = CountDownLatch(4)
        val list = mutableListOf<String>()

        val client = ComplexClient(list, latch)
        client.connect("ws://localhost:$port/eliza")
        latch.await()
        val size = list.size
        // 1. EXPLAIN WHY size = list.size IS NECESSARY
        // Para comprobar el número de mensajes (al menos) recibidos justo antes de realizar las comprobaciones.
        // 2. REPLACE BY assertXXX expression that checks an interval; assertEquals must not be used;
        assertTrue(size >= 4)
        // 3. EXPLAIN WHY assertEquals CANNOT BE USED AND WHY WE SHOULD CHECK THE INTERVAL
        // Tras el envío del mensaje por parte del cliente (test) el servidor responderá con dos mensajes, pero no hay garantías
        // de que ambos mensajes lleguen antes de que el hilo del test compruebe el número de mensajes recibidos. Recibirá al menos
        // el primero, que desbloqueará el hilo, pero el segundo mensaje podría llegar después de las comprobaciones.
        // 4. COMPLETE assertEquals(XXX, list[XXX])
        assertEquals(eliza.respond(Scanner("How are you?".lowercase(Locale.getDefault()))), list[3])
    }
}

@ClientEndpoint
class SimpleClient(
    private val list: MutableList<String>,
    private val latch: CountDownLatch,
) {
    @OnMessage
    fun onMessage(message: String) {
        logger.info { "Client received: $message" }
        list.add(message)
        latch.countDown()
    }
}

@ClientEndpoint
class ComplexClient(
    private val list: MutableList<String>,
    private val latch: CountDownLatch,
) {
    @OnMessage
    //@Suppress("UNUSED_PARAMETER") // Remove this line when you implement onMessage
    fun onMessage(
        message: String,
        session: Session,
    ) {
        logger.info { "Client received: $message" }
        list.add(message)
        latch.countDown()
        // 5. COMPLETE if (expression) {
        // 6. COMPLETE   sentence
        // }
        val count = latch.getCount()
        logger.info { "Count: $count" }
        if (latch.getCount() == 1L) {
            if (session.isOpen) {
                with(session.basicRemote) {
                    sendTextSafe("How are you?")
                }
            }
        }
    }
}

fun Any.connect(uri: String) {
    ContainerProvider.getWebSocketContainer().connectToServer(this, URI(uri))
}
