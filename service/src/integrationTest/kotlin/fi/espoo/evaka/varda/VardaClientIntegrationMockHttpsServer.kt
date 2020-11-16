// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory

class VardaClientIntegrationMockHttpsServer(httpsPort: Int) : AutoCloseable {
    val app = Javalin.create { config ->
        config.server {
            val server = Server()
            val sslConnector = ServerConnector(server, getSslContextFactory())
            sslConnector.setPort(httpsPort)
            server.setConnectors(arrayOf<Connector>(sslConnector))
            server
        }
    }.start()

    init {
        app.routes {
            get("/") { it.result("ok") }
        }
    }

    override fun close() {
        app.stop()
    }

    companion object {
        fun start(): VardaClientIntegrationMockHttpsServer {
            return VardaClientIntegrationMockHttpsServer(httpsPort = 0)
        }
    }

    fun getSslContextFactory(): SslContextFactory {
        val sslContextFactory = SslContextFactory(this.javaClass.getResource("/test-certificate/localhost.keystore").toExternalForm())
        sslContextFactory.setKeyStorePassword("password")
        return sslContextFactory
    }
}
