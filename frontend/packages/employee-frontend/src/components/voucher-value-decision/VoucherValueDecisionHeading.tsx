// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import LocalDate from '@evaka/lib-common/src/local-date'
import { Gap } from '@evaka/lib-components/src/white-space'
import { H1 } from '@evaka/lib-components/src/typography'
import LabelValueList from '~components/common/LabelValueList'
import { useTranslation } from '../../state/i18n'
import { formatDate } from '../../utils/date'
import {
  VoucherValueDecisionStatus,
  PersonDetailed
} from '../../types/invoicing'
import { getVoucherValueDecisionPdfUrl } from '../../api/invoicing'
import WarningLabel from '~components/common/WarningLabel'
import { formatName } from '~utils'
import colors from '@evaka/lib-components/src/colors'

type Props = {
  id: string
  status: VoucherValueDecisionStatus
  headOfFamily: PersonDetailed
  decisionNumber: number | null
  validFrom: LocalDate
  validTo: LocalDate | null
  sentAt: Date | null
  documentKey: string | null
  financeDecisionHandlerName: string | null
}

export default React.memo(function VoucherValueDecisionHeading({
  id,
  status,
  headOfFamily,
  decisionNumber,
  validFrom,
  validTo,
  sentAt,
  financeDecisionHandlerName,
  documentKey
}: Props) {
  const { i18n } = useTranslation()

  const pdfLink = documentKey ? (
    <a href={getVoucherValueDecisionPdfUrl(id)} download>
      {i18n.valueDecision.downloadPdf}
    </a>
  ) : (
    <DisabledLink>
      {i18n.valueDecision.downloadPdf}
      <Cursive>{i18n.valueDecision.pdfInProgress}</Cursive>
    </DisabledLink>
  )

  return (
    <>
      <TitleRow>
        <H1>{i18n.valueDecision.title[status]}</H1>
        {headOfFamily.restrictedDetailsEnabled && (
          <WarningLabel text={i18n.childInformation.restrictedDetails} />
        )}
      </TitleRow>
      <LabelValueList
        spacing="small"
        contents={[
          {
            label: i18n.valueDecision.headOfFamily,
            value: (
              <Link to={`/profile/${headOfFamily.id}`} data-qa="head-of-family">
                {formatName(
                  headOfFamily.firstName,
                  headOfFamily.lastName,
                  i18n
                )}
              </Link>
            )
          },
          {
            label: i18n.valueDecision.validPeriod,
            value: `${validFrom.format()} - ${validTo?.format() ?? ''}`
          },
          ...(sentAt
            ? [
                {
                  label: i18n.valueDecision.sentAt,
                  value: formatDate(sentAt)
                }
              ]
            : []),
          ...(decisionNumber
            ? [
                {
                  label: i18n.valueDecision.decisionNUmber,
                  value: decisionNumber
                }
              ]
            : []),
          ...(status !== 'DRAFT'
            ? [
                {
                  label: i18n.valueDecision.pdfLabel,
                  value: pdfLink
                }
              ]
            : []),
          ...(financeDecisionHandlerName
            ? [
                {
                  label: i18n.valueDecision.decisionHandlerName,
                  value: financeDecisionHandlerName
                }
              ]
            : [])
        ]}
      ></LabelValueList>
      <Gap size="L" />
    </>
  )
})

const TitleRow = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
`

const DisabledLink = styled.span`
  color: ${colors.greyscale.medium};
`

const Cursive = styled.span`
  font-style: italic;
  margin-left: 1rem;
`
