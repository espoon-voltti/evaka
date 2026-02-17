// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  deleteMobileDevice,
  getPersonalMobileDevices,
  putMobileDeviceName
} from '../../generated/api-clients/pairing'

const q = new Queries()

export const personalMobileDevicesQuery = q.query(getPersonalMobileDevices)
export const deletePersonalMobileDeviceMutation = q.mutation(
  deleteMobileDevice,
  [personalMobileDevicesQuery.prefix]
)
export const putPersonalMobileDeviceNameMutation = q.mutation(
  putMobileDeviceName,
  [personalMobileDevicesQuery.prefix]
)
