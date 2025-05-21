// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import type { UnitPreferenceFormData } from 'lib-common/api-types/application/ApplicationFormData'
import { constantQuery, useQueryResult } from 'lib-common/query'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { featureFlags } from 'lib-customizations/citizen'

import { renderResult } from '../../async-rendering'
import { serviceNeedOptionPublicInfosQuery } from '../queries'

import AdditionalDetailsSection from './AdditionalDetailsSection'
import type { ApplicationFormProps } from './ApplicationEditor'
import Heading from './Heading'
import ContactInfoSection from './contact-info/ContactInfoSection'
import ServiceNeedSection from './service-need/ServiceNeedSection'
import UnitPreferenceSection from './unit-preference/UnitPreferenceSection'

export default React.memo(function ApplicationFormDaycare({
  application,
  formData,
  setFormData,
  errors,
  verificationRequested,
  originalPreferredStartDate,
  minDate,
  maxDate
}: ApplicationFormProps) {
  const applicationType = 'DAYCARE'

  const serviceNeedOptions = useQueryResult(
    featureFlags.daycareApplication.serviceNeedOption
      ? serviceNeedOptionPublicInfosQuery({
          placementTypes: ['DAYCARE', 'DAYCARE_PART_TIME']
        })
      : constantQuery([])
  )

  const updateUnitPreferenceFormData = useCallback(
    (fn: (prev: UnitPreferenceFormData) => Partial<UnitPreferenceFormData>) =>
      setFormData((old) => ({
        ...old,
        unitPreference: {
          ...old.unitPreference,
          ...fn(old.unitPreference)
        }
      })),
    [setFormData]
  )

  return renderResult(serviceNeedOptions, (serviceNeedOptions) => (
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
        minDate={minDate}
        maxDate={maxDate}
        originalPreferredStartDate={originalPreferredStartDate}
        type={applicationType}
        formData={formData.serviceNeed}
        updateFormData={(data) =>
          setFormData((old) =>
            old
              ? {
                  ...old,
                  serviceNeed: {
                    ...old?.serviceNeed,
                    ...data
                  }
                }
              : old
          )
        }
        errors={errors.serviceNeed}
        verificationRequested={verificationRequested}
        serviceNeedOptions={serviceNeedOptions}
      />

      <UnitPreferenceSection
        formData={formData.unitPreference}
        updateFormData={updateUnitPreferenceFormData}
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
          setFormData((old) =>
            old
              ? {
                  ...old,
                  contactInfo: {
                    ...old?.contactInfo,
                    ...data
                  }
                }
              : old
          )
        }
        errors={errors.contactInfo}
        verificationRequested={verificationRequested}
        fullFamily={true}
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
          setFormData((old) =>
            old
              ? {
                  ...old,
                  additionalDetails: {
                    ...old?.additionalDetails,
                    ...data
                  }
                }
              : old
          )
        }
        errors={errors.additionalDetails}
        verificationRequested={verificationRequested}
        applicationType={applicationType}
      />
    </FixedSpaceColumn>
  ))
})
