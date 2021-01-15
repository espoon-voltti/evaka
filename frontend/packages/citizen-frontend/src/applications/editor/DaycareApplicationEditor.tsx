// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { Gap } from '@evaka/lib-components/src/white-space'
import Container from '@evaka/lib-components/src/layout/Container'
import ReturnButton from '@evaka/lib-components/src/atoms/buttons/ReturnButton'
import { useTranslation } from '~localization'
import Heading from '~applications/editor/Heading'
import ServiceNeedSection from '~applications/editor/ServiceNeedSection'
import ContactInfoSection from '~applications/editor/ContactInfoSection'
import UnitPreferenceSection from '~applications/editor/UnitPreferenceSection'
import FeeSection from '~applications/editor/FeeSection'
import AdditionalDetailsSection from '~applications/editor/AdditionalDetailsSection'
import {
  apiDataToFormData,
  ApplicationFormData,
  formDataToApiData
} from '~applications/editor/ApplicationFormData'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { getApplication } from '~applications/api'
import { Application } from '~applications/types'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { useParams } from 'react-router-dom'
import { UUID } from '@evaka/employee-frontend/src/types'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'

export default React.memo(function DaycareApplicationEditor() {
  const { applicationId } = useParams<{ applicationId: UUID }>()
  const t = useTranslation()

  const [apiData, setApiData] = useState<Result<Application>>(Loading.of())
  const [formData, setFormData] = useState<ApplicationFormData | undefined>(
    undefined
  )

  const loadApplication = useRestApi(getApplication, setApiData)
  useEffect(() => {
    loadApplication(applicationId)
  }, [applicationId])
  useEffect(() => {
    if (apiData.isSuccess) {
      setFormData(apiDataToFormData(apiData.value))
    }
  }, [apiData])

  const onSubmit = () => {
    if (!formData) return

    const reqBody = formDataToApiData(formData)
    console.log('updating application', applicationId, reqBody)
  }

  return (
    <Container>
      <ReturnButton label={t.common.return} />
      {apiData.isLoading && <SpinnerSegment />}
      {apiData.isFailure && <ErrorSegment title={'hups'} />}
      {apiData.isSuccess && formData !== undefined && (
        <>
          <Heading type="daycare" />
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
          <UnitPreferenceSection />
          <Gap size="s" />
          <ContactInfoSection />
          <Gap size="s" />
          <FeeSection />
          <Gap size="s" />
          <AdditionalDetailsSection />
          <Gap size="s" />
          <Button text={'Submitti'} onClick={onSubmit} />
        </>
      )}
    </Container>
  )
})
