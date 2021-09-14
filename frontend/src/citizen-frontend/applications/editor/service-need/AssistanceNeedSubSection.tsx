// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { useTranslation } from '../../../localization'
import { H3, Label } from 'lib-components/typography'
import TextArea from 'lib-components/atoms/form/TextArea'
import { Gap } from 'lib-components/white-space'
import { ServiceNeedSectionProps } from './ServiceNeedSection'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import styled from 'styled-components'
import { errorToInputInfo } from '../../../input-info-helper'

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

      <Gap size={'s'} />

      {type === 'PRESCHOOL' && (
        <>
          <ExpandingInfo
            info={t.applications.editor.serviceNeed.preparatoryInfo}
            ariaLabel={t.common.openExpandingInfo}
            margin="xs"
          >
            <Checkbox
              checked={formData.preparatory}
              data-qa={'preparatory-input'}
              label={t.applications.editor.serviceNeed.preparatory}
              onChange={(checked) =>
                updateFormData({
                  preparatory: checked
                })
              }
            />
          </ExpandingInfo>
          <Gap size={'m'} />
        </>
      )}

      <ExpandingInfo
        info={
          t.applications.editor.serviceNeed.assistanceNeedInstructions[type]
        }
        ariaLabel={t.common.openExpandingInfo}
        margin="xs"
      >
        <Checkbox
          checked={formData.assistanceNeeded}
          data-qa={'assistanceNeeded-input'}
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
          <Gap size={'s'} />
          <Label>
            {t.applications.editor.serviceNeed.assistanceNeedLabel + ' *'}
          </Label>
          <NarrowTextArea
            value={formData.assistanceDescription}
            data-qa={'assistanceDescription-input'}
            onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
              updateFormData({ assistanceDescription: e.target.value })
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
