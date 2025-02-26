// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import noop from 'lodash/noop'
import React from 'react'

import { DecisionId } from 'lib-common/generated/api-types/shared'
import { Button } from 'lib-components/atoms/buttons/Button'
import { faFileAlt } from 'lib-icons'

import { downloadDecisionPdf } from '../generated/api-clients/application'
import { useTranslation } from '../localization'

export const PdfLink = React.memo(function PdfLink({
  decisionId
}: {
  decisionId: DecisionId
}) {
  const t = useTranslation()

  return (
    <a
      href={downloadDecisionPdf({ id: decisionId }).url.toString()}
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
