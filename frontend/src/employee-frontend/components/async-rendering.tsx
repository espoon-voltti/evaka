// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  makeHelpers,
  RenderResultFn,
  UnwrapResultProps
} from 'lib-components/async-rendering'
import { useTranslation } from '../state/i18n'

function useFailureMessage() {
  const { i18n } = useTranslation()
  return i18n.common.loadingFailed
}

const { UnwrapResult, renderResult } = makeHelpers(useFailureMessage)
export type { UnwrapResultProps, RenderResultFn }
export { UnwrapResult, renderResult }
