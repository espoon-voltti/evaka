// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import { UnitPreferenceFormData } from 'lib-common/api-types/application/ApplicationFormData'
import { useQueryResult } from 'lib-common/query'
import Loader from 'lib-components/atoms/Loader'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

import AdditionalDetailsSection from '../../applications/editor/AdditionalDetailsSection'
import Heading from '../../applications/editor/Heading'
import ContactInfoSection from '../../applications/editor/contact-info/ContactInfoSection'
import ServiceNeedSection from '../../applications/editor/service-need/ServiceNeedSection'
import UnitPreferenceSection from '../../applications/editor/unit-preference/UnitPreferenceSection'
import { useTranslation } from '../../localization'
import { serviceNeedOptionPublicInfosQuery } from '../queries'

import { ApplicationFormProps } from './ApplicationEditor'

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
  const t = useTranslation()

  const serviceNeedOptions = useQueryResult(
    serviceNeedOptionPublicInfosQuery({
      placementTypes: ['DAYCARE', 'DAYCARE_PART_TIME']
    }),
    {
      // If service need options are not enabled, backend sets to null
      enabled: formData.serviceNeed.serviceNeedOption !== null,
      initialData: []
    }
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

  return (
    <>
      {serviceNeedOptions.isLoading && <Loader />}
      {serviceNeedOptions.isFailure && (
        <ErrorSegment title={t.common.errors.genericGetError} />
      )}
      {serviceNeedOptions.isSuccess && (
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
            serviceNeedOptions={serviceNeedOptions.value}
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
      )}
    </>
  )
})
