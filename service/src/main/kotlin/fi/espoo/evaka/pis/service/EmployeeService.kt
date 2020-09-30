// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.getEmployee
import fi.espoo.evaka.shared.db.withSpringHandle
import org.springframework.stereotype.Service
import java.util.UUID
import javax.sql.DataSource

@Service
class EmployeeService(private val dataSource: DataSource) {
    fun getEmployee(id: UUID): Employee? = withSpringHandle(dataSource) { it.getEmployee(id) }
}
