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
    val app =
        Javalin.create { config ->
                config.router.apiBuilder { get("/") { it.result("ok") } }
                config.jetty.modifyServer { server ->
                    val sslConnector = ServerConnector(server, getSslContextFactory())
                    sslConnector.port = httpsPort
                    server.connectors = arrayOf<Connector>(sslConnector)
                }
            }
            .start()

    override fun close() {
        app.stop()
    }

    companion object {
        fun start(): VardaClientIntegrationMockHttpsServer {
            return VardaClientIntegrationMockHttpsServer(httpsPort = 0)
        }
    }

    fun getSslContextFactory(): SslContextFactory.Server {
        val sslContextFactory = SslContextFactory.Server()
        sslContextFactory.keyStorePath =
            this.javaClass.getResource("/test-certificate/localhost.keystore").toExternalForm()
        sslContextFactory.setKeyStorePassword("password")
        return sslContextFactory
    }
}
