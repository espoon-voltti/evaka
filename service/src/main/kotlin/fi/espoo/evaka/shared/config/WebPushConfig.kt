// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.WebPushEnv
import fi.espoo.evaka.webpush.WebPush
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebPushConfig {
    @Bean fun webPush(env: WebPushEnv?): WebPush? = env?.let { WebPush(it) }
}
