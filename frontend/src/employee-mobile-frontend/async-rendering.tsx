// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  RenderResultFn,
  UnwrapResultProps
} from 'lib-components/async-rendering'
import { makeHelpers } from 'lib-components/async-rendering'

import { useTranslation } from './common/i18n'

function useFailureMessage() {
  const { i18n } = useTranslation()
  return {
    generic: i18n.common.loadingFailed,
    http403: i18n.common.noAccess
  }
}

const { UnwrapResult, renderResult } = makeHelpers(useFailureMessage)
export type { UnwrapResultProps, RenderResultFn }
export { UnwrapResult, renderResult }
