// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Translations } from 'lib-customizations/citizen'
import { InputInfo } from 'lib-components/atoms/form/InputField'
import { ErrorKey } from 'lib-common/form-validation'

export function errorToInputInfo(
  error: ErrorKey | undefined,
  localization: Translations['validationErrors']
): InputInfo | undefined {
  return (
    error && {
      text: localization[error],
      status: 'warning'
    }
  )
}
