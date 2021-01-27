// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Label } from '@evaka/lib-components/src/typography'
import { Gap } from '@evaka/lib-components/src/white-space'
import { TextArea } from '@evaka/lib-components/src/atoms/form/InputField'
import { useTranslation } from '~localization'
import { AdditionalDetailsFormData } from '~applications/editor/ApplicationFormData'
import EditorSection from '~applications/editor/EditorSection'
import { ApplicationType } from '@evaka/lib-common/src/api-types/application/enums'
import { ApplicationFormDataErrors } from '~applications/editor/validations'
import { getErrorCount } from '~form-validation'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'
import styled from 'styled-components'
import ExpandingInfo from '@evaka/lib-components/src/molecules/ExpandingInfo'

type Props = {
  formData: AdditionalDetailsFormData
  updateFormData: (v: Partial<AdditionalDetailsFormData>) => void
  errors: ApplicationFormDataErrors['additionalDetails']
  verificationRequested: boolean
  applicationType: ApplicationType
}

export default React.memo(function AdditionalDetailsSection({
  formData,
  updateFormData,
  verificationRequested,
  errors,
  applicationType
}: Props) {
  const t = useTranslation()

  return (
    <EditorSection
      title={t.applications.editor.additionalDetails.title}
      validationErrors={verificationRequested ? getErrorCount(errors) : 0}
      data-qa="additionalDetails-section"
    >
      <Gap size="s" />
      <FixedSpaceColumn spacing="xs">
        <Label>{t.applications.editor.additionalDetails.otherInfoLabel}</Label>
        <NarrowTextArea
          value={formData.otherInfo}
          data-qa={'otherInfo-input'}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
            updateFormData({ otherInfo: e.target.value })
          }
          placeholder={
            t.applications.editor.additionalDetails.otherInfoPlaceholder
          }
        />
      </FixedSpaceColumn>

      {applicationType !== 'CLUB' ? (
        <>
          <Gap size="L" />

          <FixedSpaceColumn spacing="xs">
            <ExpandingInfo
              info={t.applications.editor.additionalDetails.dietInfo}
            >
              <Label>{t.applications.editor.additionalDetails.dietLabel}</Label>
            </ExpandingInfo>
            <NarrowTextArea
              value={formData.diet}
              data-qa={'diet-input'}
              onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
                updateFormData({ diet: e.target.value })
              }
              placeholder={
                t.applications.editor.additionalDetails.dietPlaceholder
              }
            />
          </FixedSpaceColumn>

          <Gap size="L" />

          <FixedSpaceColumn spacing="xs">
            <ExpandingInfo
              info={t.applications.editor.additionalDetails.allergiesInfo}
            >
              <Label>
                {t.applications.editor.additionalDetails.allergiesLabel}
              </Label>
            </ExpandingInfo>
            <NarrowTextArea
              value={formData.allergies}
              data-qa={'allergies-input'}
              onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
                updateFormData({ allergies: e.target.value })
              }
              placeholder={
                t.applications.editor.additionalDetails.allergiesPlaceholder
              }
            />
          </FixedSpaceColumn>
        </>
      ) : null}
    </EditorSection>
  )
})

const NarrowTextArea = styled(TextArea)`
  max-width: 720px;
`
