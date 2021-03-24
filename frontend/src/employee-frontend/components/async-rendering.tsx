// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Result } from 'lib-common/api'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'

export function renderResult<T>(
  result: Result<T>,
  renderer: (data: T) => React.ReactNode
) {
  if (result.isLoading) return <SpinnerSegment />

  if (result.isFailure) return <ErrorSegment />

  return renderer(result.value)
}
