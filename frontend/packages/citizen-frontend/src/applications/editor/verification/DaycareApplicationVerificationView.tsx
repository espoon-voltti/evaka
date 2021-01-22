// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Container, {
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import { ApplicationFormData } from '~applications/editor/ApplicationFormData'
import { H1, P } from '~../../lib-components/src/typography'
import { useTranslation } from '~localization'
import BasicsSection from '~applications/editor/verification/BasicsSection'
import { Application } from '~applications/types'
import { Gap } from '@evaka/lib-components/src/white-space'
import HorizontalLine from '@evaka/lib-components/src/atoms/HorizontalLine'
import UnitPreferenceSection from '~applications/editor/verification/UnitPreferenceSection'

type DaycareApplicationVerificationViewProps = {
  application: Application
  formData: ApplicationFormData
}

export const DaycareApplicationVerificationLabelWidth = '240px'

const applicationType = 'daycare'

export default React.memo(function DaycareApplicationVerificationView({
  application,
  formData
}: DaycareApplicationVerificationViewProps) {
  const t = useTranslation()
  return (
    <Container>
      <ContentArea opaque>
        <H1>{t.applications.editor.verification.title[applicationType]}</H1>
        <P
          dangerouslySetInnerHTML={{
            __html: t.applications.editor.verification.notYetSent
          }}
        />
      </ContentArea>

      <Gap size="m" />

      <ContentArea opaque>
        <BasicsSection application={application} formData={formData} />
        <HorizontalLine />
        <UnitPreferenceSection formData={formData.unitPreference} />
      </ContentArea>
    </Container>
  )
})
