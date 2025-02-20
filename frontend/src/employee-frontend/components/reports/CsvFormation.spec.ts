// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import { toHeaderlessCsv } from './ReportDownload'

interface TestObject {
  firstName: string
  lastName: string
  age: number
  additionalInformation: string
}

describe('CSV formation works', () => {
  it('can handle reserved characters', () => {
    const expectedCsv =
      '"Pekka";"2";"Isokylä";"""tässä "";lisätietoja "" "\n"Teemu";"3";"Isokylä";"""tässä vielä enemmän "" lisätietoja\n"" "'

    const testObject = {
      firstName: 'Pekka',
      lastName: 'Isokylä',
      additionalInformation: '"tässä ";lisätietoja " ',
      age: 2
    }
    const testObject2 = {
      firstName: 'Teemu',
      lastName: 'Isokylä',
      additionalInformation: '"tässä vielä enemmän " lisätietoja\n" ',
      age: 3
    }
    const testHeaders: (keyof TestObject)[] = [
      'firstName',
      'age',
      'lastName',
      'additionalInformation'
    ]

    const formedCSV = toHeaderlessCsv([testObject, testObject2], testHeaders)

    expect(formedCSV).toEqual(expectedCsv)
  })
})
