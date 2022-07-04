// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'

import { Loading, Result, Success } from 'lib-common/api'
import { ServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Loader from 'lib-components/atoms/Loader'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

import AdditionalDetailsSection from '../../applications/editor/AdditionalDetailsSection'
import FeeSection from '../../applications/editor/FeeSection'
import Heading from '../../applications/editor/Heading'
import ContactInfoSection from '../../applications/editor/contact-info/ContactInfoSection'
import ServiceNeedSection from '../../applications/editor/service-need/ServiceNeedSection'
import UnitPreferenceSection from '../../applications/editor/unit-preference/UnitPreferenceSection'
import { useTranslation } from '../../localization'
import { getServiceNeedOptionPublicInfos } from '../api'

import { ApplicationFormProps } from './ApplicationEditor'

export default React.memo(function ApplicationFormDaycare({
  apiData,
  formData,
  setFormData,
  errors,
  verificationRequested
}: ApplicationFormProps) {
  const applicationType = 'DAYCARE'
  const t = useTranslation()

  const [serviceNeedOptions, setServiceNeedOptions] = useState<
    Result<ServiceNeedOptionPublicInfo[]>
  >(Loading.of())

  const loadServiceNeedOptions = useRestApi(
    getServiceNeedOptionPublicInfos,
    setServiceNeedOptions
  )

  // If service need options are not enabled, backend sets to null
  const shouldLoadServiceNeedOptions =
    formData.serviceNeed.serviceNeedOption !== null

  useEffect(() => {
    if (shouldLoadServiceNeedOptions) {
      void loadServiceNeedOptions(['DAYCARE', 'DAYCARE_PART_TIME'])
    } else {
      setServiceNeedOptions((prev) => (prev.isLoading ? Success.of([]) : prev))
    }
  }, [
    setServiceNeedOptions,
    loadServiceNeedOptions,
    shouldLoadServiceNeedOptions
  ])

  const originalPreferredStartDate =
    apiData.status !== 'CREATED'
      ? apiData.form.preferences.preferredStartDate
      : null

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
            updateFormData={(data) =>
              setFormData((old) =>
                old
                  ? {
                      ...old,
                      unitPreference: {
                        ...old?.unitPreference,
                        ...data
                      }
                    }
                  : old
              )
            }
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
              apiData.otherGuardianId
                ? apiData.otherGuardianLivesInSameAddress
                  ? 'SAME_ADDRESS'
                  : 'DIFFERENT_ADDRESS'
                : 'NO'
            }
          />

          <FeeSection
            applicationType={applicationType}
            formData={formData.fee}
            updateFormData={(data) =>
              setFormData((old) =>
                old
                  ? {
                      ...old,
                      fee: {
                        ...old?.fee,
                        ...data
                      }
                    }
                  : old
              )
            }
            errors={errors.fee}
            verificationRequested={verificationRequested}
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
