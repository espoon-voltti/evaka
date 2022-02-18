// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ErrorKey } from 'lib-common/form-validation'
import { InputInfo } from 'lib-components/atoms/form/InputField'
import { Translations } from 'lib-customizations/employee'

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
