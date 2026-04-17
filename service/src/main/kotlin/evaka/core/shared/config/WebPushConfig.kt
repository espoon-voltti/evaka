// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.config

import evaka.core.WebPushEnv
import evaka.core.webpush.WebPush
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebPushConfig {
    @Bean fun webPush(env: WebPushEnv?): WebPush? = env?.let { WebPush(it) }
}
