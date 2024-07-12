// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { clubTerms, Fixture, preschoolTerms } from './fixtures'

export const initializeAreaAndPersonData = async (): Promise<void> => {
  for (const preschoolTermFixture of preschoolTerms) {
    await Fixture.preschoolTerm().with(preschoolTermFixture).save()
  }
  for (const clubTermFixture of clubTerms) {
    await Fixture.clubTerm().with(clubTermFixture).save()
  }
}
