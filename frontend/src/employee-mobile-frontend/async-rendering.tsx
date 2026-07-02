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
    generic: {
      title: i18n.common.loadingFailed,
      text: i18n.common.loadingFailedInfo
    },
    http403: { title: i18n.common.noAccess, text: i18n.common.noAccessInfo },
    endpointDisabled: {
      title: i18n.common.endpointDisabled,
      text: i18n.common.endpointDisabledInfo
    },
    network: {
      title: i18n.common.networkError,
      text: i18n.common.networkErrorInfo
    }
  }
}

const { UnwrapResult, renderResult } = makeHelpers(useFailureMessage)
export type { UnwrapResultProps, RenderResultFn }
export { UnwrapResult, renderResult }
