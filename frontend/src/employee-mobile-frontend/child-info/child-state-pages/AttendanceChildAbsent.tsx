// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useMutationResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { cancelAbsenceMutation } from '../../child-attendance/queries'
import { InlineWideAsyncButton } from '../../common/components'
import { useTranslation } from '../../common/i18n'

interface Props {
  childId: UUID
  unitId: UUID
}

export default React.memo(function AttendanceChildAbsent({
  childId,
  unitId
}: Props) {
  const { i18n } = useTranslation()

  const { mutateAsync: cancelAbsence } = useMutationResult(
    cancelAbsenceMutation
  )

  return (
    <InlineWideAsyncButton
      text={i18n.attendances.actions.cancelAbsence}
      onClick={() => cancelAbsence({ unitId, childId })}
      onSuccess={() => undefined}
      data-qa="delete-attendance"
    />
  )
})
