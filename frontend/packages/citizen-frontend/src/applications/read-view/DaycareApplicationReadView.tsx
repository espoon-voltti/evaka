// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Container, {
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import { ApplicationFormData } from '~applications/editor/ApplicationFormData'
import { H1 } from '~../../lib-components/src/typography'
import { useTranslation } from '~localization'
import BasicsSection from '~applications/editor/verification/BasicsSection'
import HorizontalLine from '@evaka/lib-components/src/atoms/HorizontalLine'
import UnitPreferenceSection from '~applications/editor/verification/UnitPreferenceSection'
import { ApplicationDetails } from '@evaka/lib-common/src/api-types/application/ApplicationDetails'
import ReturnButton from '@evaka/lib-components/src/atoms/buttons/ReturnButton'

type DaycareApplicationReadViewProps = {
  application: ApplicationDetails
  formData: ApplicationFormData
}

const applicationType = 'DAYCARE'

export default React.memo(function DaycareApplicationReadView({
  application,
  formData
}: DaycareApplicationReadViewProps) {
  const t = useTranslation()

  return (
    <Container>
      <ReturnButton label={t.common.return} />
      <ContentArea opaque>
        <H1>{t.applications.editor.heading.title[applicationType]}</H1>
        <BasicsSection application={application} formData={formData} />
        <HorizontalLine />
        <UnitPreferenceSection formData={formData.unitPreference} />
      </ContentArea>
    </Container>
  )
})
