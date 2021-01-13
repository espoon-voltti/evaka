// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'

export default React.memo(function AdditionalDetailsSection() {
  return (
    <ContentArea opaque paddingVertical="L">
      Muut lisätiedot
    </ContentArea>
  )
})
