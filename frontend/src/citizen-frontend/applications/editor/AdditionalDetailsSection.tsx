// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationType } from 'lib-common/api-types/application/enums'
import { UpdateStateFn } from 'lib-common/form-state'
import TextArea from 'lib-components/atoms/form/TextArea'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import React from 'react'
import styled from 'styled-components'
import EditorSection from '../../applications/editor/EditorSection'
import { getErrorCount } from '../../form-validation'
import { useTranslation } from '../../localization'
import { AdditionalDetailsFormData } from './ApplicationFormData'
import { ApplicationFormDataErrors } from './validations'

type Props = {
  formData: AdditionalDetailsFormData
  updateFormData: UpdateStateFn<AdditionalDetailsFormData>
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
              info={t.applications.editor.additionalDetails.dietInfo()}
              ariaLabel={t.common.openExpandingInfo}
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
              ariaLabel={t.common.openExpandingInfo}
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
