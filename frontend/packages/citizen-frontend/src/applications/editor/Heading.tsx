// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Gap } from '@evaka/lib-components/src/white-space'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import { H1, P } from '@evaka/lib-components/src/typography'
import { useTranslation } from '~localization'

export default React.memo(function Heading({ type }: { type: 'DAYCARE' }) {
  const t = useTranslation()
  const infoParagraphs = t.applications.editor.heading.info[type]

  return (
    <ContentArea opaque paddingVertical="L">
      <H1
        noMargin
        dangerouslySetInnerHTML={{
          __html: t.applications.editor.heading.title[type] ?? ''
        }}
      />
      <Gap size="xs" />
      {infoParagraphs.map((paragraph, index) => (
        <P
          dangerouslySetInnerHTML={{ __html: paragraph }}
          key={index}
          fitted={index === infoParagraphs.length - 1}
        />
      ))}
    </ContentArea>
  )
})
