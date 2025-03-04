// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.linkity

import fi.espoo.evaka.shared.domain.FiniteDateRange

class MockLinkityClient(private val shifts: List<Shift> = emptyList()) : LinkityClient {
    private val workLogs: MutableList<WorkLog> = mutableListOf()

    override fun getShifts(period: FiniteDateRange): List<Shift> {
        return shifts.filter {
            it.startDateTime.toLocalDate() >= period.start &&
                it.startDateTime.toLocalDate() <= period.end
        }
    }

    override fun postWorkLogs(workLogs: Collection<WorkLog>) {
        this.workLogs.clear()
        this.workLogs.addAll(workLogs)
    }

    fun getPreviouslyPostedWorkLogs(): List<WorkLog> {
        return workLogs.toList()
    }
}
