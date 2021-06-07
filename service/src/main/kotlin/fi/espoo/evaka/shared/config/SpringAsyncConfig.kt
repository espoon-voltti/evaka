package fi.espoo.evaka.shared.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Profile("ai")
@Configuration
@EnableAsync
class SpringAsyncConfig : AsyncConfigurer {
    override fun getAsyncExecutor(): Executor? {
        return ThreadPoolTaskExecutor().also { it.initialize() }
    }
}
