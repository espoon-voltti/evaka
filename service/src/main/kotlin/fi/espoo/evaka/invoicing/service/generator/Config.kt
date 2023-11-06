// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service.generator

import fi.espoo.evaka.placement.PlacementType

/** These placement types will not provide sibling discount and will not be shown on any decision */
val ignoredPlacementTypes =
    setOf(PlacementType.CLUB, PlacementType.SCHOOL_SHIFT_CARE) + PlacementType.temporary

/**
 * When true, handles such cases where both parents in partnership are heads of some children
 * simultaneously, so that one parent is implicitly considered as head of all children, based on the
 * logic in fi.espoo.evaka.pis.determineHeadOfFamily
 */
const val mergeFamilies = true
