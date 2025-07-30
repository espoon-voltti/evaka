// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { ServiceNeedFormData } from 'lib-common/api-types/application/ApplicationFormData'
import type { UpdateStateFn } from 'lib-common/form-state'
import { getErrorCount } from 'lib-common/form-validation'
import type {
  ApplicationStatus,
  ApplicationType
} from 'lib-common/generated/api-types/application'
import type { ServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import type LocalDate from 'lib-common/local-date'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'

import { useTranslation } from '../../../localization'
import type { Term } from '../ApplicationEditor'
import EditorSection from '../EditorSection'
import type { ApplicationFormDataErrors } from '../validations'

import AssistanceNeedSubSection from './AssistanceNeedSubSection'
import PreferredStartSubSection from './PreferredStartSubSection'
import ServiceTimeSubSectionDaycare from './ServiceTimeSubSectionDaycare'
import ServiceTimeSubSectionPreschool from './ServiceTimeSubSectionPreschool'

export type ServiceNeedSectionProps = {
  status: ApplicationStatus
  isInvalidDate: ((localDate: LocalDate) => string | null) | undefined
  minDate: LocalDate
  maxDate: LocalDate
  type: ApplicationType
  formData: ServiceNeedFormData
  updateFormData: UpdateStateFn<ServiceNeedFormData>
  errors: ApplicationFormDataErrors['serviceNeed']
  verificationRequested: boolean
  terms?: Term[]
  serviceNeedOptions: ServiceNeedOptionPublicInfo[]
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
