// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type LocalDate from 'lib-common/local-date'

export const closingDateIsBeforeLastPlacementDate = (
  closingDate: LocalDate | null,
  lastPlacementDate: LocalDate | null | undefined
) =>
  closingDate !== null &&
  lastPlacementDate !== null &&
  lastPlacementDate !== undefined &&
  closingDate.isBefore(lastPlacementDate)
