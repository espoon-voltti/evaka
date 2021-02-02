// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationFormData } from '~applications/editor/ApplicationFormData'
import React from 'react'
import { useTranslation } from '~localization'
import { Label } from '@evaka/lib-components/src/typography'
import ListGrid from '@evaka/lib-components/src/layout/ListGrid'
import { ApplicationDataGridLabelWidth } from '~applications/editor/verification/const'

type Props = {
  formData: ApplicationFormData
}

export default React.memo(function ServiceNeedConnectedDaycare({
  formData
}: Props) {
  const t = useTranslation()
  const tLocal = t.applications.editor.verification.serviceNeed

  return (
    <ListGrid
      labelWidth={ApplicationDataGridLabelWidth}
      rowGap="s"
      columnGap="L"
    >
      <Label>{tLocal.connectedDaycare.label}</Label>
      <span>
        {formData.serviceNeed.connectedDaycare
          ? tLocal.connectedDaycare.withConnectedDaycare
          : tLocal.connectedDaycare.withoutConnectedDaycare}
      </span>

      {formData.serviceNeed.connectedDaycare && (
        <>
          <Label>{tLocal.dailyTime.dailyTime}</Label>
          <span>
            {formData.serviceNeed.startTime} - {formData.serviceNeed.endTime}
          </span>
        </>
      )}
    </ListGrid>
  )
})
