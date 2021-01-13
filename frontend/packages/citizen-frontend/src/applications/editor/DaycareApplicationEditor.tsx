// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
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

export default React.memo(function DaycareApplicationEditor() {
  const t = useTranslation()

  return (
    <Container>
      <ReturnButton label={t.common.return} />
      <Heading type="daycare" />
      <Gap size="s" />
      <ServiceNeedSection />
      <Gap size="s" />
      <UnitPreferenceSection />
      <Gap size="s" />
      <ContactInfoSection />
      <Gap size="s" />
      <FeeSection />
      <Gap size="s" />
      <AdditionalDetailsSection />
    </Container>
  )
})
