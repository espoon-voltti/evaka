// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationFormData } from '~applications/editor/ApplicationFormData'
import React from 'react'
import { useTranslation } from '~localization'
import { Label } from '@evaka/lib-components/src/typography'
import ListGrid from '@evaka/lib-components/src/layout/ListGrid'
import { ApplicationDataGridLabelWidth } from '~applications/editor/verification/const'

type AdditionalDetailsProps = {
  formData: ApplicationFormData
  showAllergiesAndDiet: boolean
}

export default React.memo(function AdditionalDetails({
  formData,
  showAllergiesAndDiet
}: AdditionalDetailsProps) {
  const t = useTranslation()
  return (
    <div>
      <ListGrid
        labelWidth={ApplicationDataGridLabelWidth}
        rowGap="s"
        columnGap="L"
      >
        <Label>
          {t.applications.editor.verification.additionalDetails.otherInfoLabel}
        </Label>
        <span>{formData.additionalDetails.otherInfo}</span>

        {showAllergiesAndDiet && (
          <>
            <Label>
              {
                t.applications.editor.verification.additionalDetails
                  .allergiesLabel
              }
            </Label>
            <span>{formData.additionalDetails.allergies}</span>

            <Label>
              {t.applications.editor.verification.additionalDetails.dietLabel}
            </Label>
            <span>{formData.additionalDetails.diet}</span>
          </>
        )}
      </ListGrid>
    </div>
  )
})
