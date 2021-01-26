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
import InfoBallWrapper from '~applications/InfoBallWrapper'
import { ApplicationType } from '@evaka/lib-common/src/api-types/application/enums'

type Props = {
  formData: AdditionalDetailsFormData
  updateFormData: (v: AdditionalDetailsFormData) => void
  applicationType: ApplicationType
}

export default React.memo(function AdditionalDetailsSection({
  formData,
  updateFormData,
  applicationType
}: Props) {
  const t = useTranslation()

  return (
    <EditorSection
      title={t.applications.editor.additionalDetails.title}
      validationErrors={0}
    >
      <Label>
        {t.applications.editor.additionalDetails.otherInfoLabel}
        <TextArea
          value={formData.otherInfo}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
            updateFormData({ ...formData, otherInfo: e.target.value })
          }
          placeholder={
            t.applications.editor.additionalDetails.otherInfoPlaceholder
          }
        />
      </Label>
      {applicationType !== 'CLUB' ? (
        <>
          <Gap size="L" />
          <Label>
            <InfoBallWrapper
              infoText={t.applications.editor.additionalDetails.dietInfo}
            >
              {t.applications.editor.additionalDetails.dietLabel}
            </InfoBallWrapper>
            <TextArea
              value={formData.diet}
              onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
                updateFormData({ ...formData, diet: e.target.value })
              }
              placeholder={
                t.applications.editor.additionalDetails.dietPlaceholder
              }
            />
          </Label>
          <Gap size="L" />
          <Label>
            <InfoBallWrapper
              infoText={t.applications.editor.additionalDetails.allergiesInfo}
            >
              {t.applications.editor.additionalDetails.allergiesLabel}
            </InfoBallWrapper>
            <TextArea
              value={formData.allergies}
              onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
                updateFormData({ ...formData, allergies: e.target.value })
              }
              placeholder={
                t.applications.editor.additionalDetails.allergiesPlaceholder
              }
            />
          </Label>
        </>
      ) : null}
    </EditorSection>
  )
})
