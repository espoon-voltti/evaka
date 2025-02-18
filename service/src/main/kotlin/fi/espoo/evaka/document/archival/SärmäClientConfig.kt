// SPDX-FileCopyrightText: 2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.archival

import fi.espoo.evaka.ArchiveEnv
import fi.espoo.evaka.EvakaEnv
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SärmäClientConfig {

    @Bean
    fun getSärmäClient(evakaEnv: EvakaEnv, archiveEnv: ArchiveEnv?): SärmäClientInterface {
        if (!evakaEnv.särmäEnabled || archiveEnv?.useMockClient == true) {
            return SärmäMockClient()
        }
        return SärmäHttpClient(archiveEnv)
    }
}
