// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  RenderResultFn,
  UnwrapResultProps
} from 'lib-components/async-rendering'
import { makeHelpers } from 'lib-components/async-rendering'

import { useTranslation } from './localization'

function useFailureMessage() {
  const t = useTranslation().common.errors
  return {
    generic: { title: t.genericGetError, info: t.genericGetErrorInfo },
    http403: { title: t.http403Error, info: t.http403ErrorInfo },
    endpointDisabled: {
      title: t.endpointDisabled,
      info: t.endpointDisabledInfo
    },
    network: { title: t.networkError, info: t.networkErrorInfo }
  }
}

const { UnwrapResult, renderResult } = makeHelpers(useFailureMessage)
export type { UnwrapResultProps, RenderResultFn }
export { UnwrapResult, renderResult }
