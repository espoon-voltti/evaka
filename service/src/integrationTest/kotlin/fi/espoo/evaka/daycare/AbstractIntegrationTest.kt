// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.placement.PlacementService
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.config.SharedIntegrationTestConfig
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter
import org.springframework.boot.context.TypeExcludeFilter
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import redis.clients.jedis.JedisPool
import javax.sql.DataSource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SharedIntegrationTestConfig::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ComponentScan(
    basePackages = ["fi.espoo.evaka.daycare", "fi.espoo.evaka.shared"],
    excludeFilters = [
        ComponentScan.Filter(type = FilterType.CUSTOM, classes = [TypeExcludeFilter::class]),
        ComponentScan.Filter(type = FilterType.CUSTOM, classes = [AutoConfigurationExcludeFilter::class])
    ]
)
abstract class AbstractIntegrationTest {

    @MockBean
    lateinit var redisPool: JedisPool

    @MockBean
    lateinit var placementService: PlacementService

    @Autowired
    lateinit var dataSource: DataSource

    @Autowired
    lateinit var jdbi: Jdbi

    lateinit var db: Database.Connection

    @BeforeAll
    protected fun beforeAll() {
        db = Database(jdbi).connect()
    }

    @AfterAll
    protected fun afterAll() {
        db.close()
    }

    @BeforeEach
    protected fun beforeEach() {
        val legacyDataSql = this.javaClass.getResource("/legacy_db_data.sql").readText()
        db.transaction { it.resetDatabase() }
        db.transaction { it.execute(legacyDataSql) }
    }
}
