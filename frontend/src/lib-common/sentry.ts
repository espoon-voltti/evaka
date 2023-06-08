// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as Sentry from '@sentry/browser'

const sourceFileSuffixes = ['.js', '.jsx', '.ts', '.tsx', '.mjs']

function isInlineScriptException(event: Sentry.Event): boolean {
  if (
    !event.exception ||
    !event.exception.values ||
    event.exception.values.length === 0
  ) {
    return false
  }

  const exception = event.exception.values[0]
  if (
    !exception.stacktrace ||
    !exception.stacktrace.frames ||
    exception.stacktrace.frames.length === 0
  ) {
    return false
  }

  const exceptionHasValidFilename = exception.stacktrace.frames.some(
    (frame) => {
      const filename = frame.filename
      return (
        filename !== undefined &&
        sourceFileSuffixes.some((suffix) => filename.endsWith(suffix))
      )
    }
  )

  return !exceptionHasValidFilename
}

export function sentryEventFilter(event: Sentry.Event): Sentry.Event | null {
  console.log('sentryEventFilter', event)
  if (isInlineScriptException(event)) return null
  return event
}
