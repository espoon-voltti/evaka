// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DaycareForm } from '../types'

export const resetGuardian2Info = (
  resetNameInfo: boolean,
  form: DaycareForm
): DaycareForm => {
  if (resetNameInfo) {
    form.guardian2.firstName = ''
    form.guardian2.lastName = ''
    form.guardian2.socialSecurityNumber = ''
    form.guardiansSeparated = false
  }
  form.secondGuardianHasAgreed = null
  form.guardian2.address.city = ''
  form.guardian2.address.postalCode = ''
  form.guardian2.address.street = ''

  form.guardian2.hasCorrectingAddress = null
  form.guardian2.guardianMovingDate = null
  form.guardian2.correctingAddress.city = ''
  form.guardian2.correctingAddress.postalCode = ''
  form.guardian2.correctingAddress.street = ''
  return form
}
