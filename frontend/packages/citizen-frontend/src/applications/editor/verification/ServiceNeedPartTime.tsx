// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationFormData } from '~applications/editor/ApplicationFormData'
import React from 'react'
import { useTranslation } from '~localization'
import { Label } from '@evaka/lib-components/src/typography'

type Props = {
  formData: ApplicationFormData
}

export default React.memo(function ServiceNeedPartTime({ formData }: Props) {
  const t = useTranslation()

  return (
    <>
      <Label>
        {t.applications.editor.verification.serviceNeed.dailyTime.partTime}
      </Label>
      <span>
        {formData.serviceNeed.partTime
          ? t.applications.editor.verification.serviceNeed.dailyTime
              .withPartTime
          : t.applications.editor.verification.serviceNeed.dailyTime
              .withoutPartTime}
      </span>

      <Label>
        {t.applications.editor.verification.serviceNeed.dailyTime.dailyTime}
      </Label>
      <span>
        {formData.serviceNeed.startTime} - {formData.serviceNeed.endTime}
      </span>
    </>
  )
})
