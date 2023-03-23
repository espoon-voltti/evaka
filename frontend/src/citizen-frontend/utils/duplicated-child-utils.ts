// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Translations } from 'citizen-frontend/localization'
import { Child } from 'lib-common/generated/api-types/children'
import { ReservationChild } from 'lib-common/generated/api-types/reservations'

export const toDuplicatedChildIds = (
  children: Array<Child | ReservationChild>
) =>
  children
    .filter((child) => child.duplicateOf !== null)
    .flatMap((child) => [child.id, child.duplicateOf as string])

export const formatDuplicatedChildIdentifier = (
  t: Translations,
  child: Child | ReservationChild,
  format: 'short' | 'long' = 'short'
) =>
  child.upcomingPlacementType === 'PRESCHOOL'
    ? t.common.duplicatedChild.identifier.PRESCHOOL[format]
    : t.common.duplicatedChild.identifier.DAYCARE[format]
