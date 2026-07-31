// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Result } from 'lib-common/api'
import type { DaycareId } from 'lib-common/generated/api-types/shared'

import type { RenderResultFn } from '../async-rendering'
import { renderResult } from '../async-rendering'

import { PinLogin } from './PinLogin'

export function renderPinRequiringResult<T>(
  result: Result<T>,
  unitId: DaycareId,
  renderer: RenderResultFn<T>
) {
  return result.isFailure && result.errorCode === 'PIN_LOGIN_REQUIRED' ? (
    <PinLogin unitId={unitId} />
  ) : (
    renderResult(result, renderer)
  )
}
