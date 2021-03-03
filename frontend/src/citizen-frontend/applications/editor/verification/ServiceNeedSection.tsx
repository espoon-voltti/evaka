// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationFormData } from '../../../applications/editor/ApplicationFormData'
import React from 'react'
import { useTranslation } from '../../../localization'
import { H2, H3, Label } from '@evaka/lib-components/typography'
import ListGrid from '@evaka/lib-components/layout/ListGrid'
import { ApplicationDataGridLabelWidth } from '../../../applications/editor/verification/const'
import { Gap } from '@evaka/lib-components/white-space'
import ServiceNeedConnectedDaycare from './ServiceNeedConnectedDaycare'
import ServiceNeedPreparatory from './ServiceNeedPreparatory'
import { ApplicationType } from '@evaka/lib-common/api-types/application/enums'
import {
  ServiceNeedUrgency,
  ServiceNeedShiftCare
} from './ServiceNeedAttachments'
import ServiceNeedPartTime from './ServiceNeedPartTime'
import styled from 'styled-components'

type ServiceNeedSectionProps = {
  formData: ApplicationFormData
  type: ApplicationType
}

export default React.memo(function ServiceNeedSection({
  formData,
  type
}: ServiceNeedSectionProps) {
  const t = useTranslation()

  return (
    <div>
      <H2>{t.applications.editor.verification.serviceNeed.title}</H2>

      <Gap size={'s'} />
      <H3>
        {t.applications.editor.verification.serviceNeed.startDate.title[type]}
      </H3>
      <ListGrid
        labelWidth={ApplicationDataGridLabelWidth}
        rowGap="s"
        columnGap="L"
      >
        <Label>
          {
            t.applications.editor.verification.serviceNeed.startDate
              .preferredStartDate
          }
        </Label>
        <span>{formData.serviceNeed.preferredStartDate}</span>

        {type === 'DAYCARE' && <ServiceNeedUrgency formData={formData} />}
        {type === 'CLUB' && (
          <>
            <Label>
              {t.applications.editor.verification.serviceNeed.wasOnDaycare}
            </Label>
            <span>
              {formData.serviceNeed.wasOnDaycare
                ? t.applications.editor.verification.serviceNeed.wasOnDaycareYes
                : t.applications.editor.verification.no}
            </span>

            <Label>
              {t.applications.editor.verification.serviceNeed.wasOnClubCare}
            </Label>
            <span>
              {formData.serviceNeed.wasOnClubCare
                ? t.applications.editor.verification.serviceNeed
                    .wasOnClubCareYes
                : t.applications.editor.verification.no}
            </span>
          </>
        )}
      </ListGrid>

      <Gap size={'s'} />

      {type === 'PRESCHOOL' && (
        <ServiceNeedConnectedDaycare formData={formData} />
      )}

      <ListGrid
        labelWidth={ApplicationDataGridLabelWidth}
        rowGap="s"
        columnGap="L"
      >
        {type === 'DAYCARE' && <ServiceNeedPartTime formData={formData} />}

        {(type === 'DAYCARE' ||
          (type === 'PRESCHOOL' && formData.serviceNeed.connectedDaycare)) && (
          <ServiceNeedShiftCare formData={formData} />
        )}
      </ListGrid>

      <Gap size={'s'} />
      <H3>
        {t.applications.editor.verification.serviceNeed.assistanceNeed.title}
      </H3>
      <ListGrid
        labelWidth={ApplicationDataGridLabelWidth}
        rowGap="s"
        columnGap="L"
      >
        <Label>
          {
            t.applications.editor.verification.serviceNeed.assistanceNeed
              .assistanceNeed
          }
        </Label>
        <span>
          {formData.serviceNeed.assistanceNeeded
            ? t.applications.editor.verification.serviceNeed.assistanceNeed
                .withAssistanceNeed
            : t.applications.editor.verification.serviceNeed.assistanceNeed
                .withoutAssistanceNeed}
        </span>

        {formData.serviceNeed.assistanceNeeded && (
          <>
            <Label>
              {
                t.applications.editor.verification.serviceNeed.assistanceNeed
                  .description
              }
            </Label>
            <div>
              {formData.serviceNeed.assistanceDescription
                .split('\n')
                .map((text, i) => (
                  <StyledP key={i}>{text}</StyledP>
                ))}
            </div>
          </>
        )}

        {type === 'PRESCHOOL' && <ServiceNeedPreparatory formData={formData} />}
      </ListGrid>
    </div>
  )
})

const StyledP = styled.p`
  margin-top: 0;
  margin-bottom: 4px;
`
