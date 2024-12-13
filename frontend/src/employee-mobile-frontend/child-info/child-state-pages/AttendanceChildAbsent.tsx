// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { ChildId, DaycareId } from 'lib-common/generated/api-types/shared'
import { useMutationResult } from 'lib-common/query'

import { cancelAbsenceMutation } from '../../child-attendance/queries'
import { InlineWideAsyncButton } from '../../common/components'
import { useTranslation } from '../../common/i18n'

interface Props {
  childId: ChildId
  unitId: DaycareId
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
