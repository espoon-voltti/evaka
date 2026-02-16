// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  deleteMobileDevice,
  getMobileDevices,
  putMobileDeviceName
} from '../../../generated/api-clients/pairing'

const q = new Queries()

export const mobileDevicesQuery = q.query(getMobileDevices)

export const deleteMobileDeviceMutation = q.mutation(deleteMobileDevice, [
  mobileDevicesQuery.prefix
])

export const putMobileDeviceNameMutation = q.mutation(putMobileDeviceName, [
  mobileDevicesQuery.prefix
])
