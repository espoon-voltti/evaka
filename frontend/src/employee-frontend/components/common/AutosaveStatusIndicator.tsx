// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { InformationText } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'
import type { AutosaveStatus } from '../../utils/use-autosave'

export default React.memo(function AutosaveStatusIndicator({
  status
}: {
  status: AutosaveStatus
}) {
  const { i18n } = useTranslation()

  function formatStatus(status: AutosaveStatus): string | null {
    switch (status.state) {
      case 'loading-error':
        return i18n.common.error.unknown
      case 'save-error':
        return i18n.common.error.saveFailed
      case 'loading':
      case 'loading-dirty':
      case 'saving':
      case 'saving-dirty':
      case 'dirty':
      case 'clean':
        return status.savedAt
          ? `${i18n.common.saved}\n${status.savedAt.toLocalTime().format()}`
          : null
    }
  }

  return (
    <InformationText data-status={status.state} data-qa="autosave-indicator">
      {formatStatus(status)}
    </InformationText>
  )
})
