// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.getAuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.configureJdbi
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.shared.dev.runDevScript
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.domain.Unauthorized
import fi.espoo.evaka.shared.utils.asArgumentResolver
import fi.espoo.evaka.shared.utils.convertFrom
import fi.espoo.evaka.vtjclient.service.persondetails.IPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import io.opentracing.Tracer
import io.opentracing.noop.NoopTracerFactory
import jakarta.servlet.http.HttpServletRequest
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.flywaydb.core.Flyway
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import org.springframework.format.FormatterRegistry
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.context.request.WebRequest.SCOPE_REQUEST
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.function.ServerRequest

val logger = mu.KotlinLogging.logger {}

private data class TestDb(
    val dataSource: HikariDataSource,
    val jdbi: Jdbi,
    val lastUsed: HelsinkiDateTime
)

class TestDbFactory {
    private val stateLock = ReentrantLock()
    private val templateDbName = "evaka_test_template"
    private val testDbPrefix = "evaka_test_db_"
    private var nextId = 0
    private val testDbs: MutableMap<Int, TestDb> = mutableMapOf()

    fun dataSourceConfig(name: String) =
        HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://localhost:5432/$name"
            username = "evaka_it"
            password = "evaka_it"
        }

    fun initializeTemplateDb() {
        HikariDataSource(dataSourceConfig("postgres")).use { ds ->
            Database(Jdbi.create(ds), NoopTracerFactory.create()).connect { db ->
                db.getRawHandle().also { handle ->
                    // Delete all test databases
                    handle
                        .createQuery("SELECT datname FROM pg_database WHERE datname LIKE :prefix")
                        .bind("prefix", "${testDbPrefix}%")
                        .mapTo<String>()
                        .list()
                        .forEach { handle.createUpdate("DROP DATABASE $it").execute() }

                    // Create the template database (if it doesn't exist)
                    val templateExists =
                        handle
                            .createQuery("SELECT true FROM pg_database WHERE datname = :name")
                            .bind("name", templateDbName)
                            .mapTo<Boolean>()
                            .firstOrNull() ?: false
                    if (!templateExists) {
                        handle.createUpdate("CREATE DATABASE $templateDbName").execute()
                    }
                }
            }
        }

        HikariDataSource(dataSourceConfig(templateDbName)).use { ds ->
            Flyway.configure()
                .apply {
                    pluginRegister
                        .getPlugin(PostgreSQLConfigurationExtension::class.java)
                        .isTransactionalLock = false
                }
                .dataSource(ds)
                .placeholders(
                    mapOf("application_user" to "evaka_it", "migration_user" to "evaka_it")
                )
                .load()
                .run { migrate() }
            Database(Jdbi.create(ds), NoopTracerFactory.create()).connect { db ->
                db.transaction { tx ->
                    tx.runDevScript("reset-database.sql")
                    tx.resetDatabase()
                }
            }
        }
    }

    private fun dbName(id: Int) = "${testDbPrefix}${id}"

    fun createTestDb(prevDbId: Int?): Int =
        stateLock.withLock {
            if (prevDbId != null) {
                testDbs.remove(prevDbId)?.also { db -> db.dataSource.close() }
            }

            val id = nextId
            nextId++
            HikariDataSource(dataSourceConfig("postgres")).use { ds ->
                Database(Jdbi.create(ds), NoopTracerFactory.create()).connect { db ->
                    db.getRawHandle().also { handle ->
                        handle
                            .createUpdate("CREATE DATABASE ${dbName(id)} TEMPLATE $templateDbName")
                            .execute()
                    }
                }
            }
            return id
        }

    fun jdbi(id: Int): Jdbi =
        stateLock.withLock {
            val threshold = HelsinkiDateTime.now().minusMinutes(2)
            testDbs.entries.mapNotNull {
                if (it.value.lastUsed < threshold) it.key else null
            }
            .forEach { testDbs.remove(it)?.also { db -> db.dataSource.close() } }

            val entry = testDbs[id]
            return if (entry != null) {
                testDbs[id] = entry.copy(lastUsed = HelsinkiDateTime.now())
                entry.jdbi
            } else {
                val dataSource = HikariDataSource(dataSourceConfig(dbName(id)))
                val jdbi = configureJdbi(Jdbi.create(dataSource))
                testDbs[id] = TestDb(dataSource, jdbi, HelsinkiDateTime.now())
                jdbi
            }
        }
}

class MockPersonDetailsServiceFactory {
    val stateLock = ReentrantLock()
    val services: MutableMap<Int, MockPersonDetailsService> = mutableMapOf()

    fun get(id: Int): MockPersonDetailsService =
        stateLock.withLock { services.getOrPut(id) { MockPersonDetailsService() } }
}

/**
 * Adds support for using the following types as REST function parameters:
 * - `Database`: a request-scoped database instance
 * - `Database.Connection`: a request-scoped database connection, closed automatically at request
 *   completion (regardless of success or failure)
 * - `AuthenticatedUser`: user performing the request
 * - `ExternalId`: an external id, is automatically parsed from a string value (e.g. path variable /
 *   query parameter, depending on annotations)
 * - `Id<*>`: a type-safe identifier, which is serialized/deserialized as UUID (= string)
 */
@Configuration
class SpringMvcConfig(
    private val jdbi: Jdbi,
    private val testDbFactory: TestDbFactory,
    private val personDetailsService: IPersonDetailsService,
    private val tracer: Tracer,
    private val env: EvakaEnv
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(asArgumentResolver<AuthenticatedUser.Citizen?>(::resolveAuthenticatedUser))
        resolvers.add(asArgumentResolver<AuthenticatedUser.Employee?>(::resolveAuthenticatedUser))
        resolvers.add(
            asArgumentResolver<AuthenticatedUser.Integration?>(::resolveAuthenticatedUser)
        )
        resolvers.add(
            asArgumentResolver<AuthenticatedUser.MobileDevice?>(::resolveAuthenticatedUser)
        )
        resolvers.add(
            asArgumentResolver<AuthenticatedUser.SystemInternalUser?>(::resolveAuthenticatedUser)
        )
        resolvers.add(asArgumentResolver<AuthenticatedUser?>(::resolveAuthenticatedUser))
        resolvers.add(asArgumentResolver { _, webRequest -> webRequest.getDatabaseInstance() })
        resolvers.add(asArgumentResolver { _, webRequest -> webRequest.getPersonDetailsService() })
        resolvers.add(asArgumentResolver { _, webRequest -> webRequest.getEvakaClock() })
    }

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(convertFrom<String, ExternalId> { ExternalId.parse(it) })
        registry.addConverter(convertFrom<String, Id<*>> { Id<DatabaseTable>(UUID.fromString(it)) })
    }

    private fun WebRequest.getDatabaseInstance(): Database {
        getDatabase()?.let {
            return it
        }
        val dbId = this.getHeader("EvakaDatabaseId")?.toIntOrNull()
        val jdbi =
            if (dbId != null) {
                logger.info("DATABASE: test $dbId")
                testDbFactory.jdbi(dbId)
            } else {
                logger.info("DATABASE: default")
                jdbi
            }
        return Database(jdbi, tracer).also(::setDatabase)
    }

    private inline fun <reified T : AuthenticatedUser> resolveAuthenticatedUser(
        parameter: MethodParameter,
        webRequest: NativeWebRequest
    ): T? {
        val user =
            webRequest.getNativeRequest(HttpServletRequest::class.java)?.getAuthenticatedUser()
                as? T
        if (user == null && !parameter.isOptional) {
            throw Unauthorized("Unauthorized request (${webRequest.getDescription(false)})")
        }
        return user
    }

    private fun WebRequest.getEvakaClock(): EvakaClock =
        if (!env.mockClock) {
            RealEvakaClock()
        } else {
            val mockTime =
                this.getHeader("EvakaMockedTime")?.let {
                    HelsinkiDateTime.from(ZonedDateTime.parse(it))
                }
            MockEvakaClock(mockTime ?: HelsinkiDateTime.now())
        }
}

private const val ATTR_DB = "evaka.database"

private fun WebRequest.getDatabase() = getAttribute(ATTR_DB, SCOPE_REQUEST) as Database?

private fun WebRequest.setDatabase(db: Database) = setAttribute(ATTR_DB, db, SCOPE_REQUEST)

private const val ATTR_PERSON_DETAILS_SERVICE = "evaka.person_details_service"

private fun WebRequest.getPersonDetailsService() =
    getAttribute(ATTR_PERSON_DETAILS_SERVICE, SCOPE_REQUEST) as IPersonDetailsService?

private fun WebRequest.setPersonDetailsService(personDetailsService: IPersonDetailsService) =
    setAttribute(ATTR_PERSON_DETAILS_SERVICE, personDetailsService, SCOPE_REQUEST)

inline fun <reified T : AuthenticatedUser?> ServerRequest.getAuthenticatedUser(): T =
    if (null is T) {
        servletRequest().getAuthenticatedUser() as? T ?: (null as T)
    } else {
        servletRequest().getAuthenticatedUser() as? T ?: throw Unauthorized("Unauthorized request")
    }
