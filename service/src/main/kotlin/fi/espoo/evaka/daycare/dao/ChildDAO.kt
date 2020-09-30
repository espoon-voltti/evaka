// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.dao

import fi.espoo.evaka.daycare.createChild
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.daycare.service.Child
import fi.espoo.evaka.daycare.updateChild
import fi.espoo.evaka.shared.db.withSpringHandle
import org.springframework.stereotype.Component
import java.util.UUID
import javax.sql.DataSource

@Component
class ChildDAO(private val dataSource: DataSource) {

    fun getChild(id: UUID): Child? = withSpringHandle(dataSource) { it.getChild(id) }

    fun createChild(child: Child): Child = withSpringHandle(dataSource) { it.createChild(child) }

    fun updateChild(child: Child) = withSpringHandle(dataSource) { it.updateChild(child) }
}
