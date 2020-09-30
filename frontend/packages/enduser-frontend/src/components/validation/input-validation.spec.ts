// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as V from './input-validation'

// TODO: add more tests for each validator

describe('input validation', () => {
  it('validate nested values', () => {
    const result = V.validate(
      'field.name',
      'fieldName',
      {
        field: {
          name: 'field name is defined'
        }
      },
      [V.required]
    )

    expect(result[0] instanceof V.ValidationSuccess).toEqual(true)
  })
})
