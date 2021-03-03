// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTranslation } from '../../../localization'

export default React.memo(function ContactInfoSecondGuardianDaycare() {
  const t = useTranslation()
  const tLocal = t.applications.editor.verification.contactInfo

  return <span>{tLocal.secondGuardian.info}</span>
})
