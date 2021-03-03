// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationFormData } from '../../../applications/editor/ApplicationFormData'
import React from 'react'
import { useTranslation } from '../../../localization'
import { Label } from '@evaka/lib-components/src/typography'

type Props = {
  formData: ApplicationFormData
}

export default React.memo(function ServiceNeedPreparatory({ formData }: Props) {
  const t = useTranslation()
  const tLocal = t.applications.editor.verification.serviceNeed

  return (
    <>
      <Label>{tLocal.preparatoryEducation.label}</Label>
      <span>
        {formData.serviceNeed.preparatory
          ? tLocal.preparatoryEducation.withPreparatory
          : tLocal.preparatoryEducation.withoutPreparatory}
      </span>
    </>
  )
})
