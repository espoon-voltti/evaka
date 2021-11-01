// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationFormData } from 'lib-common/api-types/application/ApplicationFormData'
import React from 'react'
import { useTranslation } from '../../../localization'
import { Label } from 'lib-components/typography'
import { featureFlags } from 'lib-customizations/employee'

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
      {formData.serviceNeed.serviceNeedOption == null && (
        <span>
          {formData.serviceNeed.partTime
            ? t.applications.editor.verification.serviceNeed.dailyTime
                .withPartTime
            : t.applications.editor.verification.serviceNeed.dailyTime
                .withoutPartTime}
        </span>
      )}
      {formData.serviceNeed.serviceNeedOption !== null && (
        <span>{formData.serviceNeed.serviceNeedOption.name}</span>
      )}
      {featureFlags.daycareApplication.dailyTimesEnabled && (
        <>
          <Label>
            {t.applications.editor.verification.serviceNeed.dailyTime.dailyTime}
          </Label>
          <span>
            {formData.serviceNeed.startTime} - {formData.serviceNeed.endTime}
          </span>
        </>
      )}
    </>
  )
})
