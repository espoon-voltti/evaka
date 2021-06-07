package fi.espoo.evaka.ai

import fi.espoo.evaka.ai.model.AppliedChild
import fi.espoo.evaka.ai.model.EvakaUnit
import fi.espoo.evaka.ai.model.Population
import fi.espoo.evaka.ai.model.Result
import fi.espoo.evaka.ai.model.getChildren
import fi.espoo.evaka.ai.model.getUnitData
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Conflict
import kotlinx.coroutines.runBlocking
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

val targetDate = LocalDate.of(2021, 8, 15)

@Profile("ai")
@RestController
@RequestMapping("/ai")
class AIController(
    private val service: AIService
) {

    @GetMapping("/status")
    fun getStatus(
        user: AuthenticatedUser
    ): ResponseEntity<Status> {
        user.requireOneOfRoles(UserRole.ADMIN)

        return ResponseEntity.ok(
            Status(
                running = service.running,
                generations = service.generations
            )
        )
    }

    @PostMapping("/start")
    fun postStart(
        db: Database.Connection,
        user: AuthenticatedUser
    ): ResponseEntity<Unit> {
        user.requireOneOfRoles(UserRole.ADMIN)

        db.transaction {
            val applications = it.createQuery("SELECT count(*) FROM application").mapTo<Int>().one()
            if (applications == 0) {
                generateData(it, targetDate, 300)
            }
        }

        db.read { service.start(it) }

        service.run()

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/stop")
    fun postStop(
        user: AuthenticatedUser
    ): ResponseEntity<Unit> {
        user.requireOneOfRoles(UserRole.ADMIN)

        service.stop()

        return ResponseEntity.noContent().build()
    }
}

data class Status(
    val running: Boolean,
    val generations: List<Generation>
)

data class Generation(
    val generation: Int,
    val value: Result,
    val cost: Double
)

@Profile("ai")
@Service
class AIService {
    var running = false
    var counter = 0
    val generations = mutableListOf<Generation>()
    lateinit var data: Pair<List<EvakaUnit>, List<AppliedChild>>

    fun start(tx: Database.Read) {
        if (running) throw Conflict("already running")

        generations.clear()
        counter = 0
        data = getUnitsAndChildren(tx, targetDate)
        running = true
    }

    fun stop() {
        running = false
    }

    @Async
    open fun run() {
        val (units, children) = data
        val population = Population(units, children)
        runBlocking {
            while (running) {
                counter++
                population.advance()
                generations.add(
                    Generation(
                        generation = counter,
                        value = population.getBest(),
                        cost = population.getMinimumCost()
                    )
                )
                if (counter > 1000) running = false
            }
        }
    }

    private fun getUnitsAndChildren(tx: Database.Read, date: LocalDate): Pair<List<EvakaUnit>, List<AppliedChild>> {
        val units = getUnitData(tx, date)
        val children = getChildren(tx, date, units)
        return Pair(units, children)
    }
}
