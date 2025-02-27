package fr.xibalba.p2pMusicJam

import com.shepeliev.webrtckmp.*
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64

val iceServers = listOf(IceServer(urls = listOf("stun:stun.l.google.com:19302")))
val config = RtcConfiguration(iceServers = iceServers)

class WebRTC {
    var peerConnection: PeerConnection? = null
    var dataChannel: DataChannel? = null

    suspend fun createOffer() {
        println("Create connection")
        peerConnection = PeerConnection(config)

        println("Create data channel")
        dataChannel = peerConnection!!.createDataChannel("syncChannel")
        println("Set up data channel")
        setupDataChannel()

        println("Create offer")
        val description = peerConnection!!.createOffer(OfferAnswerOptions())
        println("Set local description")
        peerConnection!!.setLocalDescription(description)
    }

    suspend fun createAnswer(offer: String) {
        val offer = Base64.decode(offer).decodeToString()
        peerConnection = PeerConnection(config)

        AppCoroutineScope.launch {
            peerConnection!!.onDataChannel.collect { channel ->
                dataChannel = channel
                setupDataChannel()
            }
        }

        peerConnection!!.setRemoteDescription(SessionDescription(SessionDescriptionType.Offer, offer))
        val answer = peerConnection!!.createAnswer(OfferAnswerOptions())
        peerConnection!!.setLocalDescription(answer)
    }

    suspend fun connect(answer: String) {
        val answer = Base64.decode(answer).decodeToString()
        if (peerConnection == null) {
            throw IllegalStateException("createOffer() or createAnswer() must be called before connect()")
        }
        peerConnection!!.setRemoteDescription(SessionDescription(SessionDescriptionType.Answer, answer))
    }

    fun setupDataChannel() {
        AppCoroutineScope.launch {
            dataChannel?.onOpen?.collect {
                console.log("DataChannel Opened!")
            }
        }
        AppCoroutineScope.launch {
            dataChannel?.onClose?.collect {
                dataChannel = null
            }
        }
    }

    fun sendData(data: String): Boolean {
        dataChannel?.send(data.encodeToByteArray())
        return dataChannel != null
    }

    fun getSpd(): String {
        return Base64.encode(peerConnection?.localDescription?.sdp?.encodeToByteArray() ?: byteArrayOf())
    }
}