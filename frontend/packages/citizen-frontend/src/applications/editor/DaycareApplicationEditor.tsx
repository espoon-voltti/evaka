// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import { Gap } from '@evaka/lib-components/src/white-space'
import Container from '@evaka/lib-components/src/layout/Container'
import Heading from '~applications/editor/Heading'
import ServiceNeedSection from '~applications/editor/ServiceNeedSection'
import ContactInfoSection from '~applications/editor/ContactInfoSection'
import UnitPreferenceSection from '~applications/editor/UnitPreferenceSection'
import FeeSection from '~applications/editor/FeeSection'
import AdditionalDetailsSection from '~applications/editor/AdditionalDetailsSection'
import {
  AdditionalDetailsFormData,
  apiDataToFormData,
  ApplicationFormData,
  FeeFormData,
  formDataToApiData
} from '~applications/editor/ApplicationFormData'
import { Application } from '~applications/types'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'

type DaycareApplicationEditorProps = {
  apiData: Application
}

const applicationType = 'daycare'

export default React.memo(function DaycareApplicationEditor({
  apiData
}: DaycareApplicationEditorProps) {
  const [formData, setFormData] = useState<ApplicationFormData>(
    apiDataToFormData(apiData)
  )

  const updateFeeFormData = useCallback(
    (feeData: FeeFormData) =>
      setFormData((previousState) => ({
        ...previousState,
        fee: feeData
      })),
    [setFormData]
  )

  const updateAdditionalDetailsFormData = useCallback(
    (additionalDetails: AdditionalDetailsFormData) =>
      setFormData((previousState) => ({
        ...previousState,
        additionalDetails: additionalDetails
      })),
    [setFormData]
  )

  const onSubmit = () => {
    if (!formData) return

    const reqBody = formDataToApiData(formData)
    console.log('updating application', apiData.id, reqBody)
  }

  return (
    <Container>
      <Heading type={applicationType} />
      <Gap size="s" />
      <ServiceNeedSection
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
      />
      <Gap size="s" />
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
      />
      <Gap size="s" />
      <ContactInfoSection
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
      />
      <Gap size="s" />
      <FeeSection formData={formData.fee} updateFormData={updateFeeFormData} />
      <Gap size="s" />
      <AdditionalDetailsSection
        formData={formData.additionalDetails}
        updateFormData={updateAdditionalDetailsFormData}
        applicationType={applicationType}
      />
      <Gap size="s" />
      <Button text={'Submitti'} onClick={onSubmit} />
    </Container>
  )
})
