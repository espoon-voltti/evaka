// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { ApplicationFormData } from 'lib-common/api-types/application/ApplicationFormData'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../../localization'

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
