// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type * as Sentry from '@sentry/browser'

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
    exception.stacktrace.frames.length <= 1
  ) {
    return false
  }

  const exceptionHasValidFilename = exception.stacktrace.frames
    .slice(1)
    .some(
      (frame) => frame.filename !== undefined && frame.filename.endsWith('.js')
    )

  return !exceptionHasValidFilename
}

export function sentryEventFilter(event: Sentry.Event): Sentry.Event | null {
  if (isInlineScriptException(event)) return null
  return event
}
