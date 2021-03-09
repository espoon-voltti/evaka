// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationFormData } from '../../../applications/editor/ApplicationFormData'
import React from 'react'
import { useTranslation } from '../../../localization'
import { H2, Label } from '@evaka/lib-components/typography'
import ListGrid from '@evaka/lib-components/layout/ListGrid'
import { ApplicationDataGridLabelWidth } from '../../../applications/editor/verification/const'
import { Gap } from '@evaka/lib-components/white-space'
import styled from 'styled-components'

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
        <div>
          {formData.additionalDetails.otherInfo.split('\n').map((text, i) => (
            <StyledP key={i}>{text}</StyledP>
          ))}
        </div>

        {showAllergiesAndDiet && (
          <>
            <Label>{tLocal.allergiesLabel}</Label>
            <div>
              {formData.additionalDetails.allergies
                .split('\n')
                .map((text, i) => (
                  <StyledP key={i}>{text}</StyledP>
                ))}
            </div>

            <Label>{tLocal.dietLabel}</Label>
            <div>
              {formData.additionalDetails.diet.split('\n').map((text, i) => (
                <StyledP key={i}>{text}</StyledP>
              ))}
            </div>
          </>
        )}
      </ListGrid>
    </div>
  )
})

const StyledP = styled.p`
  margin-top: 0;
  margin-bottom: 4px;
`
