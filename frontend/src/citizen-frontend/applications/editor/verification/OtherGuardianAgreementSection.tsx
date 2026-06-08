// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { H2, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../../localization'

export default React.memo(function OtherGuardianAgreementSection() {
  const t = useTranslation()
  const tLocal = t.applications.editor.verification.otherGuardianAgreement
  return (
    <div data-qa="other-guardian-agreement-section">
      <H2 $noMargin>{tLocal.title}</H2>
      <Gap $size="m" />
      <P $noMargin>{tLocal.text}</P>
    </div>
  )
})
