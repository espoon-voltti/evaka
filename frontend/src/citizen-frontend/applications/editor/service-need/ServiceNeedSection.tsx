// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import { UpdateStateFn } from 'lib-common/form-state'
import LocalDate from 'lib-common/local-date'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import React from 'react'
import EditorSection from '../../../applications/editor/EditorSection'
import AssistanceNeedSubSection from '../../../applications/editor/service-need/AssistanceNeedSubSection'
import PreferredStartSubSection from '../../../applications/editor/service-need/PreferredStartSubSection'
import ServiceTimeSubSectionDaycare from '../../../applications/editor/service-need/ServiceTimeSubSectionDaycare'
import ServiceTimeSubSectionPreschool from '../../../applications/editor/service-need/ServiceTimeSubSectionPreschool'
import { getErrorCount } from 'lib-common/form-validation'
import { useTranslation } from '../../../localization'
import { ServiceNeedFormData } from 'lib-common/api-types/application/ApplicationFormData'
import { ApplicationFormDataErrors, Term } from '../validations'
import { ApplicationStatus, ApplicationType } from 'lib-common/generated/enums'

export type ServiceNeedSectionProps = {
  status: ApplicationStatus
  originalPreferredStartDate: LocalDate | null
  type: ApplicationType
  formData: ServiceNeedFormData
  updateFormData: UpdateStateFn<ServiceNeedFormData>
  errors: ApplicationFormDataErrors['serviceNeed']
  verificationRequested: boolean
  terms?: Term[]
  serviceNeedOptions?: ServiceNeedOptionPublicInfo[]
}

export default React.memo(function ServiceNeedSection(
  props: ServiceNeedSectionProps
) {
  const t = useTranslation()

  return (
    <EditorSection
      title={t.applications.editor.serviceNeed.title}
      validationErrors={
        props.verificationRequested ? getErrorCount(props.errors) : 0
      }
      openInitially
      data-qa="serviceNeed-section"
    >
      <PreferredStartSubSection {...props} />
      <HorizontalLine />

      {props.type === 'DAYCARE' && (
        <>
          <ServiceTimeSubSectionDaycare {...props} />
          <HorizontalLine />
        </>
      )}

      {props.type === 'PRESCHOOL' && (
        <>
          <ServiceTimeSubSectionPreschool {...props} />
          <HorizontalLine />
        </>
      )}

      <AssistanceNeedSubSection {...props} />
    </EditorSection>
  )
})
