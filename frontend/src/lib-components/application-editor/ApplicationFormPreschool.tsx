// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

import AdditionalDetailsSection from './AdditionalDetailsSection'
import Heading from './Heading'
import ContactInfoSection from './contact-info/ContactInfoSection'
import {
  useApplicationServiceNeedOptions,
  useSectionUpdaters
} from './formState'
import ServiceNeedSection from './service-need/ServiceNeedSection'
import type { ApplicationFormProps } from './types'
import UnitPreferenceSection from './unit-preference/UnitPreferenceSection'

export default React.memo(function ApplicationFormPreschool({
  deps,
  application,
  formData,
  setFormData,
  errors,
  verificationRequested,
  alertTrigger,
  isInvalidDate,
  minDate,
  maxDate,
  terms
}: ApplicationFormProps) {
  const { renderResult } = deps
  const applicationType = 'PRESCHOOL'
  const update = useSectionUpdaters(setFormData)

  const serviceNeedOptions = useApplicationServiceNeedOptions(deps, application)

  return renderResult(serviceNeedOptions, (serviceNeedOptions) => (
    <FixedSpaceColumn $spacing="s">
      <Heading
        deps={deps}
        type={applicationType}
        transferApplication={application.transferApplication}
        firstName={application.form.child.person.firstName}
        lastName={application.form.child.person.lastName}
        errors={verificationRequested ? errors : undefined}
        alertTrigger={alertTrigger}
      />

      <ServiceNeedSection
        deps={deps}
        applicationId={application.id}
        status={application.status}
        isInvalidDate={isInvalidDate}
        minDate={minDate}
        maxDate={maxDate}
        type={applicationType}
        formData={formData.serviceNeed}
        updateFormData={update.serviceNeed}
        errors={errors.serviceNeed}
        verificationRequested={verificationRequested}
        terms={terms}
        serviceNeedOptions={serviceNeedOptions}
      />

      <UnitPreferenceSection
        deps={deps}
        formData={formData.unitPreference}
        updateFormData={update.unitPreference}
        applicationType={applicationType}
        preparatory={formData.serviceNeed.preparatory}
        preferredStartDate={formData.serviceNeed.preferredStartDate}
        errors={errors.unitPreference}
        verificationRequested={verificationRequested}
        shiftCare={formData.serviceNeed.shiftCare}
      />

      <ContactInfoSection
        deps={deps}
        type={applicationType}
        application={application}
        formData={formData.contactInfo}
        updateFormData={update.contactInfo}
        errors={errors.contactInfo}
        verificationRequested={verificationRequested}
        fullFamily={formData.serviceNeed.connectedDaycare}
        otherGuardianStatus={
          application.hasOtherGuardian
            ? application.otherGuardianLivesInSameAddress
              ? 'SAME_ADDRESS'
              : 'DIFFERENT_ADDRESS'
            : 'NO'
        }
      />

      <AdditionalDetailsSection
        deps={deps}
        formData={formData.additionalDetails}
        updateFormData={update.additionalDetails}
        errors={errors.additionalDetails}
        verificationRequested={verificationRequested}
        applicationType={applicationType}
      />
    </FixedSpaceColumn>
  ))
})
