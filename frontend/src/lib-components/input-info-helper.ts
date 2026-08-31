// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ErrorKey } from 'lib-common/form-validation'
import type { InputInfo } from 'lib-components/atoms/form/InputField'

export function errorToInputInfo(
  error: ErrorKey | undefined,
  localization: Record<ErrorKey, string>
): InputInfo | undefined {
  return (
    error && {
      text: localization[error],
      status: 'warning'
    }
  )
}
