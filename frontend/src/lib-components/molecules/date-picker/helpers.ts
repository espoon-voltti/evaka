// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

export const nativeDatePickerEnabled = (() => {
  const nativeDatePickerAgentMatchers = [/Android/i]

  if (!nativeDatePickerAgentMatchers.some((m) => m.test(navigator.userAgent))) {
    return false
  }

  const input = document.createElement('input')
  input.setAttribute('type', 'date')

  const testDate = LocalDate.of(2020, 2, 11)
  input.setAttribute('value', testDate.formatIso())

  return (
    !!input.valueAsDate &&
    LocalDate.fromSystemTzDate(input.valueAsDate).isEqual(testDate)
  )
})()
