// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { ApplicationFormData } from 'lib-common/api-types/application/ApplicationFormData'
import ListGrid from 'lib-components/layout/ListGrid'
import { Label } from 'lib-components/typography'
import { ApplicationDataGridLabelWidth } from '../../../applications/editor/verification/const'
import { useTranslation } from '../../../localization'

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
