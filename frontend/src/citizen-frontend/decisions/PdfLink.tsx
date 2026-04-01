// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type { DecisionId } from 'lib-common/generated/api-types/shared'
import { InlineExternalLinkButton } from 'lib-components/atoms/buttons/InlineLinkButton'
import { faFileAlt } from 'lib-icons'

import { downloadDecisionPdf } from '../generated/api-clients/application'
import { useTranslation } from '../localization'

const WrappingLinkButton = styled(InlineExternalLinkButton)`
  white-space: normal;
`

export const PdfLink = React.memo(function PdfLink({
  decisionId,
  text
}: {
  decisionId: DecisionId
  text?: string
}) {
  const t = useTranslation()

  return (
    <WrappingLinkButton
      href={downloadDecisionPdf({ id: decisionId }).url.toString()}
      icon={faFileAlt}
      text={text ?? t.decisions.applicationDecisions.openPdf}
      newTab={true}
      data-qa="button-open-pdf"
    />
  )
})
