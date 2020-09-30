// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createJwt } from './src/shared/auth/jwt'

const token = createJwt({
  kind: 'EspooAD',
  // user UUID
  sub: '70c93dbc-217d-11ea-84c9-1bc36b5e1f74',
  // list of roles that are added to the user separated by space
  // includes all roles copied from a comment in daycare-service
  scope:
    'ROLE_EVAKA_ESPOO_USER ROLE_EVAKA_ESPOO_READER ROLE_EVAKA_ESPOO_OFFICEHOLDER ROLE_EVAKA_ESPOO_FINANCEADMIN ROLE_EVAKA_ESPOO_VEO ROLE_EVAKA_ESPOO_EDUCATOR ROLE_EVAKA_ESPOO_UNITSUPERVISOR ROLE_EVAKA_ESPOO_ADMIN'
})

console.log(`
Dev token:
${token}
`)
