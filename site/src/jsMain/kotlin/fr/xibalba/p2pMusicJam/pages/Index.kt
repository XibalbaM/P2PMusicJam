package fr.xibalba.p2pMusicJam.pages

import androidx.compose.runtime.*
import com.shepeliev.webrtckmp.onDataChannel
import com.varabyte.kobweb.compose.css.height
import com.varabyte.kobweb.compose.css.width
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.forms.TextInput
import fr.xibalba.p2pMusicJam.AppCoroutineScope
import fr.xibalba.p2pMusicJam.WebRTC
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea


@Page
@Composable
fun HomePage() {
    val webRTC by remember { mutableStateOf(WebRTC()) }
    var mode by remember { mutableStateOf(Mode.NONE) }
    val chat = remember { mutableStateListOf<String>() }

    Column {
        H1 {
            Text("P2P Chat")
        }

        when (mode) {
            Mode.NONE -> {
                Row {
                    Button(onClick = {
                        AppCoroutineScope.launch {
                                webRTC.createOffer()
                                mode = Mode.OFFER
                        }
                    }) {
                        Text("Create Offer")
                    }

                    Button(onClick = {
                        mode = Mode.ANSWER
                    }) {
                        Text("Create Answer")
                    }
                }
            }
            Mode.OFFER -> {
                var answer by remember { mutableStateOf("") }
                Div {
                    Text("Offer :")
                    Button(onClick = {
                        window.navigator.clipboard.writeText(webRTC.getSpd())
                    }) {
                        Text("Copy")
                    }
                }
                Text("Answer : ")
                TextArea(answer) {
                    width(550)
                    height(250)
                    onInput {
                        answer = it.value
                    }
                }
                Button(onClick = {
                    AppCoroutineScope.launch {
                        webRTC.connect(answer)
                        mode = Mode.CONNECTED
                    }
                }) {
                    Text("Connect")
                }
            }
            Mode.ANSWER -> {
                var offer by remember { mutableStateOf("") }
                Text("Offer : ")
                TextArea(offer) {
                    width(550)
                    height(250)
                    onInput {
                        offer = it.value
                    }
                }
                Button(onClick = {
                    AppCoroutineScope.launch {
                        webRTC.createAnswer(offer)
                        mode = Mode.ANSWERED
                    }
                }) {
                    Text("Answer")
                }
            }
            Mode.ANSWERED -> {
                Div {
                    Text("Answer :")
                    Button(onClick = {
                        window.navigator.clipboard.writeText(webRTC.getSpd())
                    }) {
                        Text("Copy")
                    }
                }
                AppCoroutineScope.launch {
                    webRTC.peerConnection?.onDataChannel?.collect {
                        mode = Mode.CONNECTED
                    }
                }
            }
            Mode.CONNECTED -> {
                Div {
                    for (item in chat) {
                        P {
                            Text(item)
                        }
                    }
                }
                var message by remember { mutableStateOf("") }
                TextInput(message, onTextChange = {
                    message = it
                })
                Button(onClick = {
                    AppCoroutineScope.launch {
                        webRTC.sendData(message)
                        chat.add("You said: $message")
                        message = ""
                    }
                }) {
                    Text("Send")
                }

                AppCoroutineScope.launch {
                    webRTC.dataChannel?.onClose?.collect {
                        mode = Mode.NONE
                    }
                }
            }
        }
    }

    LaunchedEffect(mode, webRTC) {
        if (mode == Mode.CONNECTED) {
            AppCoroutineScope.launch {
                webRTC.dataChannel?.onMessage?.collect {
                    chat.add("They said: ${it.decodeToString()}")
                }
            }
        }
    }
}

enum class Mode {
    NONE,
    OFFER,
    ANSWER,
    ANSWERED,
    CONNECTED
}