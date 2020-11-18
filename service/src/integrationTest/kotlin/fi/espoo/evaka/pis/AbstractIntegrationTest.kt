// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.config.SharedIntegrationTestConfig
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.utils.DisableSecurity
import fi.espoo.evaka.vtjclient.VtjIntegrationTestConfig
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter
import org.springframework.boot.context.TypeExcludeFilter
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

@DisableSecurity
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SharedIntegrationTestConfig::class, VtjIntegrationTestConfig::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ComponentScan(
    basePackages = ["fi.espoo.evaka.pis", "fi.espoo.evaka.shared", "fi.espoo.evaka.vtjclient"],
    excludeFilters = [
        ComponentScan.Filter(type = FilterType.CUSTOM, classes = [TypeExcludeFilter::class]),
        ComponentScan.Filter(type = FilterType.CUSTOM, classes = [AutoConfigurationExcludeFilter::class])
    ]
)
abstract class AbstractIntegrationTest {

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
        db.transaction { it.resetDatabase() }
    }
}
