// SPDX-FileCopyrightText: 2023-2025 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.vtjclient

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.ws.transport.WebServiceMessageSender
import org.springframework.ws.transport.http.HttpComponentsMessageSender

@Configuration
@Profile("integration-test")
class TestConfig {
    @Bean fun webServiceMessageSender(): WebServiceMessageSender = HttpComponentsMessageSender()
}
