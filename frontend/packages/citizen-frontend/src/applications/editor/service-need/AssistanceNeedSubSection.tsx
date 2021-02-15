// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import { useTranslation } from '~localization'
import { H3 } from '@evaka/lib-components/src/typography'
import { TextArea } from '@evaka/lib-components/src/atoms/form/InputField'
import { Gap } from '@evaka/lib-components/src/white-space'
import { ServiceNeedSectionProps } from '~applications/editor/service-need/ServiceNeedSection'
import ExpandingInfo from '@evaka/lib-components/src/molecules/ExpandingInfo'
import styled from 'styled-components'

export default React.memo(function AssistanceNeedSubSection({
  type,
  formData,
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

          <NarrowTextArea
            value={formData.assistanceDescription}
            data-qa={'assistanceDescription-input'}
            onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
              updateFormData({ assistanceDescription: e.target.value })
            }
            placeholder={
              t.applications.editor.serviceNeed.assistanceNeedPlaceholder
            }
          />
        </>
      )}
    </>
  )
})

const NarrowTextArea = styled(TextArea)`
  max-width: 720px;
`
