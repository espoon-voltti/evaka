// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.linkity

import fi.espoo.evaka.shared.domain.FiniteDateRange

class MockLinkityClient(private val shifts: List<Shift> = emptyList()) : LinkityClient {
    private var previousBatch: StampingBatch? = null

    override fun getShifts(period: FiniteDateRange): List<Shift> {
        return shifts.filter {
            it.startDateTime.toLocalDate() >= period.start &&
                it.startDateTime.toLocalDate() <= period.end
        }
    }

    override fun postStampings(batch: StampingBatch) {
        previousBatch = batch
    }

    fun getPreviouslyPostedStampings(): StampingBatch? {
        return previousBatch
    }
}
