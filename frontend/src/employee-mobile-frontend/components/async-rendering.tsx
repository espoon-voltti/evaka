// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import {
  makeHelpers,
  RenderResultFn,
  UnwrapResultProps
} from 'lib-components/async-rendering'
import { useTranslation } from '../state/i18n'
import { Result } from 'lib-common/api'

function useFailureMessage() {
  const { i18n } = useTranslation()
  return i18n.common.loadingFailed
}

const { UnwrapResult } = makeHelpers(useFailureMessage)
export type { UnwrapResultProps, RenderResultFn }
export { UnwrapResult }

export function renderResult<T>(
  result: Result<T>,
  renderer: RenderResultFn<T>
) {
  return <UnwrapResult result={result}>{renderer}</UnwrapResult>
}
