import { Result } from 'lib-common/api'
import React from 'react'
import { SpinnerSegment } from './atoms/state/Spinner'
import ErrorSegment from './atoms/state/ErrorSegment'

export function renderResult<T>(
  result: Result<T>,
  renderer: (data: T) => React.ReactElement | null
) {
  if (result.isLoading) return <SpinnerSegment />

  if (result.isFailure) return <ErrorSegment />

  return renderer(result.value)
}
