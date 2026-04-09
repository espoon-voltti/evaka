// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka.time

import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.RealEvakaClock
import org.springframework.stereotype.Service

@Service
class ClockService {
    fun clock(): EvakaClock = RealEvakaClock()
}
