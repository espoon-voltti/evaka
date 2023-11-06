// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import Checkbox from 'lib-components/atoms/form/Checkbox'
import TextArea from 'lib-components/atoms/form/TextArea'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { H3, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'

import { errorToInputInfo } from '../../../input-info-helper'
import { useTranslation } from '../../../localization'

import { ServiceNeedSectionProps } from './ServiceNeedSection'

export default React.memo(function AssistanceNeedSubSection({
  type,
  formData,
  errors,
  verificationRequested,
  updateFormData
}: ServiceNeedSectionProps) {
  const t = useTranslation()

  return (
    <>
      <H3>{t.applications.editor.serviceNeed.assistanceNeed}</H3>

      <Gap size="s" />

      {type === 'PRESCHOOL' && featureFlags.preparatory && (
        <>
          <ExpandingInfo
            info={t.applications.editor.serviceNeed.preparatoryInfo}
          >
            <Checkbox
              checked={formData.preparatory}
              data-qa="preparatory-input"
              label={t.applications.editor.serviceNeed.preparatory}
              onChange={(checked) =>
                updateFormData({
                  preparatory: checked
                })
              }
            />
          </ExpandingInfo>
          <Gap size="m" />
        </>
      )}

      <ExpandingInfo
        info={
          t.applications.editor.serviceNeed.assistanceNeedInstructions[type]
        }
        data-qa={`assistanceNeedInstructions-${type}`}
      >
        <Checkbox
          checked={formData.assistanceNeeded}
          data-qa="assistanceNeeded-input"
          label={t.applications.editor.serviceNeed.assistanceNeeded}
          onChange={(checked) =>
            updateFormData({
              assistanceNeeded: checked
            })
          }
        />
      </ExpandingInfo>

      {formData.assistanceNeeded && (
        <>
          <Gap size="s" />
          <Label>
            {t.applications.editor.serviceNeed.assistanceNeedLabel + ' *'}
          </Label>
          <NarrowTextArea
            value={formData.assistanceDescription}
            data-qa="assistanceDescription-input"
            onChange={(value) =>
              updateFormData({ assistanceDescription: value })
            }
            placeholder={
              t.applications.editor.serviceNeed.assistanceNeedPlaceholder
            }
            hideErrorsBeforeTouched={!verificationRequested}
            info={errorToInputInfo(
              errors.assistanceDescription,
              t.validationErrors
            )}
            required={true}
          />
        </>
      )}
    </>
  )
})

const NarrowTextArea = styled(TextArea)`
  max-width: 720px;
`
