// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { DecisionId } from 'lib-common/generated/api-types/shared'
import { InlineExternalLinkButton } from 'lib-components/atoms/buttons/InlineLinkButton'
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
    <InlineExternalLinkButton
      href={downloadDecisionPdf({ id: decisionId }).url.toString()}
      icon={faFileAlt}
      text={t.decisions.applicationDecisions.openPdf}
      newTab={true}
      data-qa="button-open-pdf"
    />
  )
})
