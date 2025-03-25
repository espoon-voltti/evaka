// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.linkity

import fi.espoo.evaka.shared.domain.FiniteDateRange

class MockLinkityClient(private val shifts: List<Shift> = emptyList()) : LinkityClient {
    private val stampings: MutableList<Stamping> = mutableListOf()

    override fun getShifts(period: FiniteDateRange): List<Shift> {
        return shifts.filter {
            it.startDateTime.toLocalDate() >= period.start &&
                it.startDateTime.toLocalDate() <= period.end
        }
    }

    override fun postStampings(stampings: Collection<Stamping>) {
        this.stampings.clear()
        this.stampings.addAll(stampings)
    }

    fun getPreviouslyPostedStampingss(): List<Stamping> {
        return stampings.toList()
    }
}
