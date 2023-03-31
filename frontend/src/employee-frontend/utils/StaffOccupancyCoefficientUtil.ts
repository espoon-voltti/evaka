// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { STAFF_OCCUPANCY_COEFFICIENT } from 'employee-frontend/constants'

export abstract class StaffOccupancyCoefficientUtil {
  public static parseToBoolean(numberValue?: number) {
    return numberValue !== undefined && numberValue > 0
  }

  public static parseToNumber(booleanValue: boolean) {
    return booleanValue ? STAFF_OCCUPANCY_COEFFICIENT : 0
  }

  public static parseNumberValue(numberValue?: number) {
    return numberValue ?? 0
  }
}
