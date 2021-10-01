import React from 'react'
import { Result } from 'lib-common/api'
import {
  genericUnwrapResult,
  UnwrapResultProps
} from 'lib-components/async-rendering'
import { useTranslation } from './localization'

export function UnwrapResult<T>(props: UnwrapResultProps<T>) {
  const t = useTranslation()
  return genericUnwrapResult({
    ...props,
    failureMessage: t.common.errors.genericGetError
  })
}

export function renderResult<T>(
  result: Result<T>,
  renderer: (value: T) => React.ReactElement | null
) {
  return <UnwrapResult result={result}>{renderer}</UnwrapResult>
}
