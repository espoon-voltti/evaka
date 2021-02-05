// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
@Profile("local")
class DevDataInitializer(jdbi: Jdbi) {
    init {
        jdbi.transaction { h ->
            if (h.createQuery("SELECT count(*) FROM care_area").mapTo<Int>().first() == 0) {
                ClassPathResource("dev-data/espoo-dev-data.sql").inputStream.use {
                    it.bufferedReader().readText().let { content ->
                        h.createUpdate(content).execute()
                    }
                }
                ClassPathResource("dev-data/employees.sql").inputStream.use {
                    it.bufferedReader().readText().let { content ->
                        h.createUpdate(content).execute()
                    }
                }
                ClassPathResource("dev-data/preschool-terms.sql").inputStream.use {
                    it.bufferedReader().readText().let { content ->
                        h.createUpdate(content).execute()
                    }
                }
            }
        }
    }
}
