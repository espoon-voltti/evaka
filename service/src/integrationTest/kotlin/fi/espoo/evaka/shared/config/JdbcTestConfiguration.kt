// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.utils.DisableSecurity
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@DisableSecurity
@TestConfiguration
@PropertySource("classpath:integration-test-common.properties")
class JdbcTestConfiguration {
    @Bean
    fun dataSource(): DataSource = getTestDataSource()

    @Bean
    fun txManager(dataSource: DataSource): PlatformTransactionManager = DataSourceTransactionManager(dataSource)
}
