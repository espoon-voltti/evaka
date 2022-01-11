// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import { formatDate } from 'lib-common/date'
import {
  VoucherValueDecisionDetailed,
  VoucherValueDecisionType
} from 'lib-common/generated/api-types/invoicing'
import { H1 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { getVoucherValueDecisionPdfUrl } from '../../api/invoicing'
import LabelValueList from '../../components/common/LabelValueList'
import WarningLabel from '../../components/common/WarningLabel'
import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'
import { TypeSelect } from '../fee-decision-details/TypeSelect'

type Props = {
  decision: VoucherValueDecisionDetailed
  changeDecisionType: (type: VoucherValueDecisionType) => void
  newDecisionType: VoucherValueDecisionType
}

export default React.memo(function VoucherValueDecisionHeading({
  decision: {
    id,
    status,
    headOfFamily,
    partner,
    decisionNumber,
    validFrom,
    validTo,
    sentAt,
    financeDecisionHandlerFirstName,
    financeDecisionHandlerLastName,
    decisionType,
    documentKey,
    isElementaryFamily
  },
  changeDecisionType,
  newDecisionType
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

  const reliefValue =
    status === 'DRAFT' ? (
      <TypeSelect
        changeDecisionType={changeDecisionType}
        selected={newDecisionType}
        type="VALUE_DECISION"
      />
    ) : (
      i18n.valueDecision.type[decisionType]
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
          ...(partner && isElementaryFamily
            ? [
                {
                  label: i18n.valueDecision.partner,
                  value: (
                    <Link to={`/profile/${partner.id}`} data-qa="partner">
                      {formatName(partner.firstName, partner.lastName, i18n)}
                    </Link>
                  )
                }
              ]
            : []),
          {
            label: i18n.valueDecision.validPeriod,
            value: `${validFrom.format()} - ${validTo?.format() ?? ''}`
          },
          {
            label: i18n.valueDecision.relief,
            value: reliefValue
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
          ...(financeDecisionHandlerFirstName && financeDecisionHandlerLastName
            ? [
                {
                  label: i18n.valueDecision.decisionHandlerName,
                  value: `${financeDecisionHandlerFirstName} ${financeDecisionHandlerLastName}`
                }
              ]
            : [])
        ]}
      />
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
