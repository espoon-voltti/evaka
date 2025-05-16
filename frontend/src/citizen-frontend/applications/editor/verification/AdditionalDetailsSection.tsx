// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type { ApplicationFormData } from 'lib-common/api-types/application/ApplicationFormData'
import ListGrid from 'lib-components/layout/ListGrid'
import { H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../../localization'

import { ApplicationDataGridLabelWidth } from './const'

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
