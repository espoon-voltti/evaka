// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext } from 'react'

import { Result } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { faQuestion } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'

interface Props {
  title: string
  range: DateRange
  onSubmit: () => Promise<Result<void>>
}

export const DeleteConfirmation = React.memo(function DeleteConfirmation({
  title,
  range,
  onSubmit
}: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode, setErrorMessage } = useContext(UIContext)

  const resolveAction = useCallback(() => {
    onSubmit()
      .then(() => {
        clearUiMode()
      })
      .catch(() => {
        setErrorMessage({
          type: 'error',
          title: i18n.common.error.unknown,
          text: i18n.common.error.unknown,
          resolveLabel: i18n.common.ok
        })
      })
  }, [i18n, onSubmit, clearUiMode, setErrorMessage])

  return (
    <InfoModal
      type="warning"
      title={title}
      text={`${i18n.common.period} ${range.format()}`}
      icon={faQuestion}
      reject={{ action: () => clearUiMode(), label: i18n.common.cancel }}
      resolve={{
        action: resolveAction,
        label: i18n.common.remove
      }}
    />
  )
})
