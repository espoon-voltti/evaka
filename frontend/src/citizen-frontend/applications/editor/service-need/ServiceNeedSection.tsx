// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { ServiceNeedFormData } from '../../../applications/editor/ApplicationFormData'
import { useTranslation } from '../../../localization'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import EditorSection from '../../../applications/editor/EditorSection'
import { ApplicationFormDataErrors } from '../../../applications/editor/validations'
import { getErrorCount } from '../../../form-validation'
import PreferredStartSubSection from '../../../applications/editor/service-need/PreferredStartSubSection'
import AssistanceNeedSubSection from '../../../applications/editor/service-need/AssistanceNeedSubSection'
import {
  ApplicationStatus,
  ApplicationType
} from 'lib-common/api-types/application/enums'
import ServiceTimeSubSectionDaycare from '../../../applications/editor/service-need/ServiceTimeSubSectionDaycare'
import ServiceTimeSubSectionPreschool from '../../../applications/editor/service-need/ServiceTimeSubSectionPreschool'
import LocalDate from 'lib-common/local-date'

export type ServiceNeedSectionProps = {
  status: ApplicationStatus
  originalPreferredStartDate: LocalDate | null
  type: ApplicationType
  formData: ServiceNeedFormData
  updateFormData: (update: Partial<ServiceNeedFormData>) => void
  errors: ApplicationFormDataErrors['serviceNeed']
  verificationRequested: boolean
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
