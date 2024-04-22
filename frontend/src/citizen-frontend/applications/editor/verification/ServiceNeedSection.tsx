// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { ApplicationFormData } from 'lib-common/api-types/application/ApplicationFormData'
import { ApplicationType } from 'lib-common/generated/api-types/application'
import ListGrid from 'lib-components/layout/ListGrid'
import { H2, H3, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'

import { useTranslation } from '../../../localization'

import {
  ServiceNeedUrgency,
  ServiceNeedShiftCare
} from './ServiceNeedAttachments'
import ServiceNeedConnectedDaycare from './ServiceNeedConnectedDaycare'
import ServiceNeedPartTime from './ServiceNeedPartTime'
import ServiceNeedPreparatory from './ServiceNeedPreparatory'
import { ApplicationDataGridLabelWidth } from './const'

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
    <div data-qa="service-need-section">
      <H2>{t.applications.editor.verification.serviceNeed.title}</H2>

      <Gap size="s" />
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
        <span>{formData.serviceNeed.preferredStartDate?.format()}</span>

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

      <Gap size="s" />

      {type === 'PRESCHOOL' && (
        <>
          <ServiceNeedConnectedDaycare formData={formData} />
          <Gap size="s" />
        </>
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

      <Gap size="s" />
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
            <div data-qa="assistance-need-description">
              {formData.serviceNeed.assistanceDescription
                .split('\n')
                .map((text, i) => (
                  <StyledP key={i}>{text}</StyledP>
                ))}
            </div>
          </>
        )}

        {type === 'PRESCHOOL' && featureFlags.preparatory && (
          <ServiceNeedPreparatory formData={formData} />
        )}
      </ListGrid>
    </div>
  )
})

const StyledP = styled.p`
  margin-top: 0;
  margin-bottom: 4px;
`
