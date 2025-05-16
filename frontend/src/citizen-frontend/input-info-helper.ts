// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ErrorKey } from 'lib-common/form-validation'
import type { InputInfo } from 'lib-components/atoms/form/InputField'
import type { Translations } from 'lib-customizations/citizen'

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
