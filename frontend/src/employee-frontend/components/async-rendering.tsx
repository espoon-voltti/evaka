// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Result } from 'lib-common/api'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { useTranslation } from 'employee-frontend/state/i18n'
import Loader from 'lib-components/atoms/Loader'

export function renderResult<T>(
  result: Result<T>,
  renderer: (data: T) => React.ReactNode
) {
  if (result.isLoading) return <SpinnerSegment />

  if (result.isFailure) return <ErrorSegment />

  return renderer(result.value)
}

export function UnwrapResult<T>({
  children,
  result
}: {
  children: (data: T) => JSX.Element
  result: Result<T>
}) {
  const { i18n } = useTranslation()

  if (result.isLoading) {
    return <Loader />
  } else if (result.isFailure) {
    return <div>{i18n.common.loadingFailed}</div>
  } else {
    return children(result.value)
  }
}
