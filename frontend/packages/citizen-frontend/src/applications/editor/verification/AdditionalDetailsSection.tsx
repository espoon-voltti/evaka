// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationFormData } from '~applications/editor/ApplicationFormData'
import React from 'react'
import { useTranslation } from '~localization'
import { H2, Label } from '@evaka/lib-components/src/typography'
import ListGrid from '@evaka/lib-components/src/layout/ListGrid'
import { ApplicationDataGridLabelWidth } from '~applications/editor/verification/const'
import { Gap } from '@evaka/lib-components/src/white-space'

type AdditionalDetailsProps = {
  formData: ApplicationFormData
  showAllergiesAndDiet: boolean
}

export default React.memo(function AdditionalDetails({
  formData,
  showAllergiesAndDiet
}: AdditionalDetailsProps) {
  const t = useTranslation()
  const tLocal = t.applications.editor.verification.additionalDetails
  return (
    <div>
      <H2 noMargin>{tLocal.title}</H2>
      <Gap size="s" />
      <ListGrid
        labelWidth={ApplicationDataGridLabelWidth}
        rowGap="s"
        columnGap="L"
      >
        <Label>{tLocal.otherInfoLabel}</Label>
        <span>{formData.additionalDetails.otherInfo}</span>

        {showAllergiesAndDiet && (
          <>
            <Label>{tLocal.allergiesLabel}</Label>
            <span>{formData.additionalDetails.allergies}</span>

            <Label>{tLocal.dietLabel}</Label>
            <span>{formData.additionalDetails.diet}</span>
          </>
        )}
      </ListGrid>
    </div>
  )
})
