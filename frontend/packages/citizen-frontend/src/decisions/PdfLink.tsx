// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import InlineButton from '@evaka/lib-components/src/atoms/buttons/InlineButton'
import { faFileAlt } from '@evaka/lib-icons'
import { noop } from 'lodash'
import React from 'react'
import { useTranslation } from '~localization'

export const PdfLink = React.memo(function PdfLink({
  decisionId
}: {
  decisionId: string
}) {
  const t = useTranslation()

  return (
    <a
      href={`/api/application/citizen/decisions/${decisionId}/download`}
      target="_blank"
      rel="noreferrer"
    >
      <InlineButton
        icon={faFileAlt}
        text={t.decisions.applicationDecisions.openPdf}
        onClick={noop}
        dataQa="button-open-pdf"
      />
    </a>
  )
})
