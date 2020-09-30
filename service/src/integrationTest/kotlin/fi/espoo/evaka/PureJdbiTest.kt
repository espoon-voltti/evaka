// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.shared.config.getTestDataSource
import fi.espoo.evaka.shared.db.configureJdbi
import fi.espoo.evaka.shared.db.handle
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class PureJdbiTest {
    protected lateinit var jdbi: Jdbi

    @BeforeAll
    protected fun initializeJdbi() {
        jdbi = configureJdbi(Jdbi.create(getTestDataSource()))
        jdbi.handle(::resetDatabase)
    }
}
