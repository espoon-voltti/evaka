// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export const personIdentity = {
  id: '123e4567-e89b-12d3-a456-426655440000',
  socialSecurityNumber: '070644-937X'
}

export const person = {
  firstName: 'Seppo',
  lastName: 'Sorsa',
  socialSecurityNumber: personIdentity.socialSecurityNumber,
  streetAddress: 'Kamreerintie 1',
  postalCode: '02770',
  city: 'Espoo',
  id: personIdentity.id,
  children: []
}
