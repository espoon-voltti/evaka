// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTranslation } from '~localization'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'
import { H3 } from '@evaka/lib-components/src/typography'

export default React.memo(function SecondGuardianSubSection() {
  const t = useTranslation()
  return (
    <>
      <H3>{t.applications.editor.contactInfo.secondGuardianInfoTitle}</H3>
      <FixedSpaceColumn spacing={'xs'}>
        <p>{t.applications.editor.contactInfo.secondGuardianInfo}</p>
      </FixedSpaceColumn>
    </>
  )
})
