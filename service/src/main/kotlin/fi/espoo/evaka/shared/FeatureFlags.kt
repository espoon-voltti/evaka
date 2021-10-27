// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

data class FeatureFlags(
    val valueDecisionCapacityFactorEnabled: Boolean,
    val daycareApplicationServiceNeedOptionsEnabled: Boolean
)
