// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { toCsv } from './csv'

describe('toCsv', () => {
  it('works', () => {
    const csv = toCsv(
      [
        {
          firstName: 'Pekka',
          lastName: 'Iso;kylä',
          additionalInformation: 'sekä "että" kä',
          age: 2
        },
        {
          firstName: 'Tee-\nmu',
          lastName: 'Isokylä',
          additionalInformation: 'foo;bar;"baz"',
          age: 3
        }
      ],
      [
        { label: 'Name', value: (row) => row.firstName },
        { label: '"Age"', value: (row) => row.age * 2 },
        { label: 'Sur;name', value: (row) => row.lastName },
        { label: 'Excluded field', value: (row) => row.age, exclude: true },
        { label: 'Info', value: (row) => row.additionalInformation }
      ]
    )
    const expected =
      [
        'Name;"""Age""";"Sur;name";Info',
        'Pekka;4;"Iso;kylä";"sekä ""että"" kä"',
        '"Tee-\nmu";6;Isokylä;"foo;bar;""baz"""'
      ].join('\n') + '\n'

    expect(csv).toEqual(expected)
  })
})
