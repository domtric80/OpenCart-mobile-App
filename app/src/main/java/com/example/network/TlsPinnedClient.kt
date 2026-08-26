package com.example.network

import android.content.Context
import okhttp3.CertificatePinner
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Per-host trust-on-first-use pinning for user-configured OpenCart installations.
 *
 * The enrollment handshake uses the platform trust store and HTTPS hostname verification. The
 * public-key pins from that validated chain are persisted and enforced by OkHttp thereafter.
 */
class TlsPinnedClient(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val baseClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    @Throws(IOException::class)
    fun execute(request: Request): Response {
        val url = request.url
        if (!url.isHttps) {
            throw IOException("CartAdmin richiede HTTPS per tutte le connessioni OpenCart")
        }

        val pins = getOrEnrollPins(url)
        val pinnerBuilder = CertificatePinner.Builder()
        pins.forEach { pin -> pinnerBuilder.add(url.host, pin) }
        val certificatePinner = pinnerBuilder.build()
        val pinnedClient = baseClient.newBuilder()
            .certificatePinner(certificatePinner)
            .build()

        return pinnedClient.newCall(request).execute()
    }

    private fun getOrEnrollPins(url: HttpUrl): Set<String> {
        val key = pinKey(url)
        preferences.getStringSet(key, null)?.toSet()?.takeIf { it.isNotEmpty() }?.let {
            return it
        }

        synchronized(enrollmentLock) {
            preferences.getStringSet(key, null)?.toSet()?.takeIf { it.isNotEmpty() }?.let {
                return it
            }

            val enrolledPins = discoverValidatedChainPins(url.host, url.port)
            if (!preferences.edit().putStringSet(key, enrolledPins).commit()) {
                throw IOException("Impossibile salvare il pin TLS in modo persistente")
            }
            return enrolledPins
        }
    }

    private fun discoverValidatedChainPins(host: String, port: Int): Set<String> {
        val rawSocket = Socket()
        rawSocket.connect(InetSocketAddress(host, port), TLS_CONNECT_TIMEOUT_MS)

        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val socket = factory.createSocket(rawSocket, host, port, true) as SSLSocket
        socket.soTimeout = TLS_READ_TIMEOUT_MS
        socket.sslParameters = socket.sslParameters.apply {
            endpointIdentificationAlgorithm = "HTTPS"
        }

        return socket.use { tlsSocket ->
            tlsSocket.startHandshake()
            val pins = tlsSocket.session.peerCertificates
                .filterIsInstance<X509Certificate>()
                .map(CertificatePinner::pin)
                .toSet()
            if (pins.isEmpty()) {
                throw IOException("Il server non ha presentato una catena TLS valida")
            }
            pins
        }
    }

    private fun pinKey(url: HttpUrl): String = "tls_pins_${url.host}_${url.port}"

    private companion object {
        const val PREFS_NAME = "cartadmin_tls_pins"
        const val TLS_CONNECT_TIMEOUT_MS = 10_000
        const val TLS_READ_TIMEOUT_MS = 15_000
        val enrollmentLock = Any()
    }
}
