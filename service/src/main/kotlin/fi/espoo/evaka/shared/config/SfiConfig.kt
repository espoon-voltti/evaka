package fi.espoo.evaka.shared.config

import fi.espoo.evaka.SfiEnv
import fi.espoo.evaka.sficlient.ISfiClientService
import fi.espoo.evaka.sficlient.MockClientService
import fi.espoo.evaka.sficlient.SfiClientService
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SfiConfig {
    @Bean
    fun sfiClient(env: ObjectProvider<SfiEnv>): ISfiClientService = env.ifAvailable?.let {
        SfiClientService(it)
    } ?: MockClientService()
}
