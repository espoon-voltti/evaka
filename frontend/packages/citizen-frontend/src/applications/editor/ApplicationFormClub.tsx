import React, { useEffect } from 'react'

import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'
import Heading from '~applications/editor/Heading'
import ServiceNeedSection from '~applications/editor/service-need/ServiceNeedSection'
import UnitPreferenceSection from '~applications/editor/unit-preference/UnitPreferenceSection'
import LocalDate from '@evaka/lib-common/src/local-date'
import ContactInfoSection from '~applications/editor/contact-info/ContactInfoSection'
import AdditionalDetailsSection from '~applications/editor/AdditionalDetailsSection'
import { ApplicationFormProps } from '~applications/editor/ApplicationEditor'
import { useTranslation } from '~localization'

export default React.memo(function ApplicationFormClub({
  apiData,
  formData,
  setFormData,
  errors,
  verificationRequested
}: ApplicationFormProps) {
  const t = useTranslation()
  const applicationType = 'CLUB'

  useEffect(() => {
    document.title = t.applications.editor.heading.title[applicationType]
    return () => {
      document.title = t.common.title
    }
  }, [])

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
        preferredStartDate={LocalDate.parseFiOrNull(
          formData.serviceNeed.preferredStartDate
        )}
        errors={errors.unitPreference}
        verificationRequested={verificationRequested}
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
  )
})
