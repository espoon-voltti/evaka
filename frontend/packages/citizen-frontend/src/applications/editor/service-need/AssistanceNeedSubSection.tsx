// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import { useTranslation } from '~localization'
import { H3 } from '@evaka/lib-components/src/typography'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import { Gap } from '@evaka/lib-components/src/white-space'
import { errorToInputInfo } from '~form-validation'
import { ServiceNeedSectionProps } from '~applications/editor/service-need/ServiceNeedSection'
import ExpandingInfo from '@evaka/lib-components/src/molecules/ExpandingInfo'

export default React.memo(function AssistanceNeedSubSection({
  type,
  formData,
  updateFormData,
  errors,
  verificationRequested
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
          >
            {' '}
            <Checkbox
              checked={formData.preparatory}
              dataQa={'preparatory-input'}
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
        info={t.applications.editor.serviceNeed.assistanceNeedInstructions}
      >
        {' '}
        <Checkbox
          checked={formData.assistanceNeeded}
          dataQa={'assistanceNeeded-input'}
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

          <InputField
            value={formData.assistanceDescription}
            dataQa={'assistanceDescription-input'}
            onChange={(value) =>
              updateFormData({ assistanceDescription: value })
            }
            placeholder={
              t.applications.editor.serviceNeed.assistanceNeedPlaceholder
            }
            info={errorToInputInfo(
              errors.assistanceDescription,
              t.validationErrors
            )}
            hideErrorsBeforeTouched={!verificationRequested}
            width="XL"
          />
        </>
      )}
    </>
  )
})
