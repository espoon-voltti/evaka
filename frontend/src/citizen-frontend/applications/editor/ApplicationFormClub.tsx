// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

import AdditionalDetailsSection from './AdditionalDetailsSection'
import type { ApplicationFormProps } from './ApplicationEditor'
import Heading from './Heading'
import ContactInfoSection from './contact-info/ContactInfoSection'
import ServiceNeedSection from './service-need/ServiceNeedSection'
import UnitPreferenceSection from './unit-preference/UnitPreferenceSection'

export default React.memo(function ApplicationFormClub({
  application,
  formData,
  setFormData,
  errors,
  verificationRequested,
  originalPreferredStartDate,
  minDate,
  maxDate,
  terms
}: ApplicationFormProps) {
  const applicationType = 'CLUB'

  return (
    <FixedSpaceColumn spacing="s">
      <Heading
        type={applicationType}
        transferApplication={application.transferApplication}
        firstName={application.form.child.person.firstName}
        lastName={application.form.child.person.lastName}
        errors={verificationRequested ? errors : undefined}
      />

      <ServiceNeedSection
        status={application.status}
        originalPreferredStartDate={originalPreferredStartDate}
        minDate={minDate}
        maxDate={maxDate}
        type={applicationType}
        formData={formData.serviceNeed}
        updateFormData={(data) =>
          setFormData((old) => ({
            ...old,
            serviceNeed: {
              ...old.serviceNeed,
              ...data
            }
          }))
        }
        errors={errors.serviceNeed}
        verificationRequested={verificationRequested}
        terms={terms}
        serviceNeedOptions={[]}
      />

      <UnitPreferenceSection
        formData={formData.unitPreference}
        updateFormData={useCallback(
          (fn) =>
            setFormData((old) => ({
              ...old,
              unitPreference: {
                ...old.unitPreference,
                ...fn(old.unitPreference)
              }
            })),
          [setFormData]
        )}
        applicationType={applicationType}
        preparatory={false}
        preferredStartDate={formData.serviceNeed.preferredStartDate}
        errors={errors.unitPreference}
        verificationRequested={verificationRequested}
        shiftCare={formData.serviceNeed.shiftCare}
      />

      <ContactInfoSection
        type={applicationType}
        formData={formData.contactInfo}
        updateFormData={(data) =>
          setFormData((old) => ({
            ...old,
            contactInfo: {
              ...old.contactInfo,
              ...data
            }
          }))
        }
        errors={errors.contactInfo}
        verificationRequested={verificationRequested}
        fullFamily={false}
        otherGuardianStatus={
          application.hasOtherGuardian
            ? application.otherGuardianLivesInSameAddress
              ? 'SAME_ADDRESS'
              : 'DIFFERENT_ADDRESS'
            : 'NO'
        }
      />

      <AdditionalDetailsSection
        formData={formData.additionalDetails}
        updateFormData={(data) =>
          setFormData((old) => ({
            ...old,
            additionalDetails: {
              ...old.additionalDetails,
              ...data
            }
          }))
        }
        errors={errors.additionalDetails}
        verificationRequested={verificationRequested}
        applicationType={applicationType}
      />
    </FixedSpaceColumn>
  )
})
