// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router'
import styled from 'styled-components'

import type FiniteDateRange from 'lib-common/finite-date-range'
import type {
  FeeDecisionStatus,
  FeeDecisionType,
  PersonDetailed
} from 'lib-common/generated/api-types/invoicing'
import type { FeeDecisionId } from 'lib-common/generated/api-types/shared'
import type HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { H1 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { getFeeDecisionPdf } from '../../generated/api-clients/invoicing'
import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'
import LabelValueList from '../common/LabelValueList'
import WarningLabel from '../common/WarningLabel'

import { TypeSelect } from './TypeSelect'

interface Props {
  id: FeeDecisionId
  status: FeeDecisionStatus
  headOfFamily: PersonDetailed
  partner: PersonDetailed | null
  decisionNumber: number | null
  validDuring: FiniteDateRange
  sentAt: HelsinkiDateTime | null
  financeDecisionHandlerFirstName: string | null
  financeDecisionHandlerLastName: string | null
  approvedBy: { firstName: string; lastName: string } | null
  documentKey: string | null
  decisionType: FeeDecisionType
  changeDecisionType: (type: FeeDecisionType) => void
  newDecisionType: FeeDecisionType
  partnerIsCodebtor: boolean | null
}

function displayDecisionNumber(number: number) {
  return number.toString().padStart(12, '0')
}

export default React.memo(function Heading({
  id,
  status,
  headOfFamily,
  partner,
  decisionNumber,
  validDuring,
  sentAt,
  financeDecisionHandlerFirstName,
  financeDecisionHandlerLastName,
  documentKey,
  decisionType,
  changeDecisionType,
  newDecisionType,
  partnerIsCodebtor
}: Props) {
  const { i18n } = useTranslation()

  const reliefValue =
    status === 'DRAFT' ? (
      <TypeSelect
        changeDecisionType={changeDecisionType}
        selected={newDecisionType}
        type="FEE_DECISION"
      />
    ) : (
      i18n.feeDecision.type[decisionType]
    )

  const pdfValue = documentKey ? (
    <a href={getFeeDecisionPdf({ decisionId: id }).url.toString()} download>
      {i18n.feeDecision.downloadPdf}
    </a>
  ) : (
    <DisabledLink>
      {i18n.feeDecision.downloadPdf}
      <Cursive>{i18n.feeDecision.pdfInProgress}</Cursive>
    </DisabledLink>
  )

  function contents() {
    const decisionNumberExists: {
      label: React.ReactNode
      value: React.ReactNode
      valueWidth?: string
      'data-qa'?: string
    }[] = decisionNumber
      ? [
          {
            label: i18n.feeDecision.decisionNumber,
            value: displayDecisionNumber(decisionNumber)
          }
        ]
      : []

    const notDraft: {
      label: React.ReactNode
      value: React.ReactNode
      valueWidth?: string
      'data-qa'?: string
    }[] =
      status !== 'DRAFT'
        ? [
            {
              label: i18n.feeDecision.pdfLabel,
              value: pdfValue
            }
          ]
        : []

    const partnerElement: {
      label: React.ReactNode
      value: React.ReactNode
      valueWidth?: string
      'data-qa'?: string
    }[] =
      partner && partnerIsCodebtor
        ? [
            {
              label: i18n.feeDecision.partner,
              value: (
                <Link to={`/profile/${partner.id}`} data-qa="partner">
                  {formatName(partner.firstName, partner.lastName, i18n)}
                </Link>
              )
            }
          ]
        : []

    const headOfFamilyElement: {
      label: React.ReactNode
      value: React.ReactNode
      valueWidth?: string
      'data-qa'?: string
    }[] = [
      {
        label: i18n.feeDecision.headOfFamily,
        value: (
          <Link to={`/profile/${headOfFamily.id}`} data-qa="head-of-family">
            {formatName(headOfFamily.firstName, headOfFamily.lastName, i18n)}
          </Link>
        )
      }
    ]

    const always: {
      label: React.ReactNode
      value: React.ReactNode
      valueWidth?: string
      dataQa?: string
    }[] = [
      {
        dataQa: 'valid-during',
        label: i18n.feeDecision.validPeriod,
        value: validDuring.format()
      },
      {
        label: i18n.feeDecision.sentAt,
        value: sentAt?.toLocalDate().format() ?? ''
      },
      {
        label: i18n.feeDecision.relief,
        value: reliefValue
      }
    ].concat(
      financeDecisionHandlerFirstName && financeDecisionHandlerLastName
        ? [
            {
              dataQa: 'decision-handler',
              label: i18n.feeDecision.decisionHandler,
              value: `${financeDecisionHandlerFirstName} ${financeDecisionHandlerLastName}`
            }
          ]
        : []
    )

    return decisionNumberExists
      .concat(notDraft)
      .concat(headOfFamilyElement)
      .concat(partnerElement)
      .concat(always)
  }

  return (
    <>
      <TitleRow>
        <H1>{i18n.feeDecision.title[status]}</H1>
        <InfoMarkers>
          {headOfFamily.restrictedDetailsEnabled && (
            <WarningLabel text={i18n.childInformation.restrictedDetails} />
          )}
        </InfoMarkers>
      </TitleRow>
      <LabelValueList spacing="small" contents={contents()} />
      <Gap size="L" />
    </>
  )
})

export const TitleRow = styled.div`
  display: flex;
  margin-bottom: 1.5rem;
  justify-content: space-between;
`

export const InfoMarkers = styled.span``

const DisabledLink = styled.span`
  color: ${colors.grayscale.g35};
`

const Cursive = styled.span`
  font-style: italic;
  margin-left: 1rem;
`
