// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Result } from 'lib-common/api'
import {
  genericUnwrapResult,
  UnwrapResultProps
} from 'lib-components/async-rendering'
import { useTranslation } from '../state/i18n'

export function UnwrapResult<T>(props: UnwrapResultProps<T>) {
  const { i18n } = useTranslation()
  return genericUnwrapResult({
    ...props,
    failureMessage: i18n.common.loadingFailed
  })
}

export function renderResult<T>(
  result: Result<T>,
  renderer: (value: T) => React.ReactElement | null
) {
  return <UnwrapResult result={result}>{renderer}</UnwrapResult>
}
