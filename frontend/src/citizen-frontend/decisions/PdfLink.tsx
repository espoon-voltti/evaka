// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import noop from 'lodash/noop'
import React from 'react'

import { Button } from 'lib-components/atoms/buttons/Button'
import { faFileAlt } from 'lib-icons'

import { useTranslation } from '../localization'

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
      <Button
        appearance="inline"
        icon={faFileAlt}
        text={t.decisions.applicationDecisions.openPdf}
        onClick={noop}
        data-qa="button-open-pdf"
      />
    </a>
  )
})
