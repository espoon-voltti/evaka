// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import React from 'react'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'

export interface UnwrapResultProps<T> {
  result: Result<T>
  loading?: () => React.ReactElement | null
  failure?: () => React.ReactElement | null
  children?: (value: T) => React.ReactElement | null
}

interface GenericUnwrapResultOpts<T> extends UnwrapResultProps<T> {
  failureMessage: string
}

export function genericUnwrapResult<T>({
  result,
  loading,
  failure,
  children,
  failureMessage
}: GenericUnwrapResultOpts<T>) {
  if (result.isLoading) {
    return loading?.() ?? <SpinnerSegment />
  }
  if (result.isFailure) {
    return failure?.() ?? <ErrorSegment title={failureMessage} />
  }
  return children ? children(result.value) : null
}
