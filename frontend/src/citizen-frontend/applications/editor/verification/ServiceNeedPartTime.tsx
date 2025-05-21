// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { ApplicationFormData } from 'lib-common/api-types/application/ApplicationFormData'
import { Label } from 'lib-components/typography'
import { featureFlags } from 'lib-customizations/citizen'

import { useTranslation } from '../../../localization'

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
        <span>{formData.serviceNeed.serviceNeedOption.nameFi}</span>
      )}
      {featureFlags.daycareApplication.dailyTimes && (
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
