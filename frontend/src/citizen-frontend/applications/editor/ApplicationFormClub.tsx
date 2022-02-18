// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import LocalDate from 'lib-common/local-date'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

import AdditionalDetailsSection from '../../applications/editor/AdditionalDetailsSection'
import Heading from '../../applications/editor/Heading'
import ContactInfoSection from '../../applications/editor/contact-info/ContactInfoSection'
import ServiceNeedSection from '../../applications/editor/service-need/ServiceNeedSection'
import UnitPreferenceSection from '../../applications/editor/unit-preference/UnitPreferenceSection'

import { ApplicationFormProps } from './ApplicationEditor'

export default React.memo(function ApplicationFormClub({
  apiData,
  formData,
  setFormData,
  errors,
  verificationRequested,
  terms
}: ApplicationFormProps) {
  const applicationType = 'CLUB'

  const preferredStartDate = useMemo(
    () => LocalDate.parseFiOrNull(formData.serviceNeed.preferredStartDate),
    [formData.serviceNeed.preferredStartDate]
  )

  const originalPreferredStartDate =
    apiData.status !== 'CREATED'
      ? apiData.form.preferences.preferredStartDate
      : null

  return (
    <FixedSpaceColumn spacing="s">
      <Heading
        type={applicationType}
        transferApplication={apiData.transferApplication}
        firstName={apiData.form.child.person.firstName}
        lastName={apiData.form.child.person.lastName}
        errors={verificationRequested ? errors : undefined}
      />

      <ServiceNeedSection
        status={apiData.status}
        originalPreferredStartDate={originalPreferredStartDate}
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
      />

      <UnitPreferenceSection
        formData={formData.unitPreference}
        updateFormData={(data) =>
          setFormData((old) => ({
            ...old,
            unitPreference: {
              ...old.unitPreference,
              ...data
            }
          }))
        }
        applicationType={applicationType}
        preparatory={false}
        preferredStartDate={preferredStartDate}
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
          apiData.otherGuardianId
            ? apiData.otherGuardianLivesInSameAddress
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
