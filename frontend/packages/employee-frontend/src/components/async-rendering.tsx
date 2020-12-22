// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Result } from 'api'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'

export function renderResult<T>(
  result: Result<T>,
  renderer: (data: T) => React.ReactNode
) {
  if (result.isLoading) return <SpinnerSegment />

  if (result.isFailure) return <ErrorSegment />

  return renderer(result.value)
}
