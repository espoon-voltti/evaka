// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import DateRange from 'lib-common/date-range'
import { Gap } from 'lib-components/white-space'
import { H1 } from 'lib-components/typography'
import SimpleSelect from 'lib-components/atoms/form/SimpleSelect'
import LabelValueList from '../../components/common/LabelValueList'
import WarningLabel from '../../components/common/WarningLabel'
import { getFeeDecisionPdfUrl } from '../../api/invoicing'
import { useTranslation } from '../../state/i18n'
import {
  FeeDecisionStatus,
  FeeDecisionType,
  PersonDetailed
} from '../../types/invoicing'
import colors from 'lib-customizations/common'
import { formatDate } from '../../utils/date'
import { formatName } from '../../utils'

interface TypeSelectProps {
  selected: string
  changeDecisionType: (type: string) => void
}

const TypeSelect = ({ selected, changeDecisionType }: TypeSelectProps) => {
  const { i18n } = useTranslation()

  const options = [
    { value: 'NORMAL', label: i18n.feeDecision.type.NORMAL },
    {
      value: 'RELIEF_ACCEPTED',
      label: i18n.feeDecision.type.RELIEF_ACCEPTED
    },
    {
      value: 'RELIEF_PARTLY_ACCEPTED',
      label: i18n.feeDecision.type.RELIEF_PARTLY_ACCEPTED
    },
    {
      value: 'RELIEF_REJECTED',
      label: i18n.feeDecision.type.RELIEF_REJECTED
    }
  ]

  return (
    <SimpleSelect
      value={selected}
      options={options}
      onChange={(e) => changeDecisionType(e.target.value)}
    />
  )
}

interface Props {
  id: string
  status: FeeDecisionStatus
  headOfFamily: PersonDetailed
  decisionNumber: number | null
  validDuring: DateRange
  sentAt: Date | null
  financeDecisionHandlerFirstName: string | null
  financeDecisionHandlerLastName: string | null
  approvedBy: { firstName: string; lastName: string } | null
  documentKey: string | null
  decisionType: FeeDecisionType
  changeDecisionType: (type: string) => void
  newDecisionType: string
}

function displayDecisionNumber(number: number) {
  return number.toString().padStart(12, '0')
}

export default React.memo(function Heading({
  id,
  status,
  headOfFamily,
  decisionNumber,
  validDuring,
  sentAt,
  financeDecisionHandlerFirstName,
  financeDecisionHandlerLastName,
  documentKey,
  decisionType,
  changeDecisionType,
  newDecisionType
}: Props) {
  const { i18n } = useTranslation()

  const reliefValue =
    status === 'DRAFT' ? (
      <TypeSelect
        changeDecisionType={changeDecisionType}
        selected={newDecisionType}
      />
    ) : (
      i18n.feeDecision.type[decisionType]
    )

  const pdfValue = documentKey ? (
    <a href={getFeeDecisionPdfUrl(id)} download>
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

    const always: {
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
      },
      {
        label: i18n.feeDecision.validPeriod,
        value: validDuring.format()
      },
      {
        label: i18n.feeDecision.sentAt,
        value: formatDate(sentAt)
      },
      {
        label: i18n.feeDecision.relief,
        value: reliefValue
      }
    ].concat(
      financeDecisionHandlerFirstName && financeDecisionHandlerLastName
        ? [
            {
              label: i18n.feeDecision.decisionHandler,
              value: `${financeDecisionHandlerFirstName} ${financeDecisionHandlerLastName}`
            }
          ]
        : []
    )

    return decisionNumberExists.concat(notDraft).concat(always)
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
  color: ${colors.greyscale.medium};
`

const Cursive = styled.span`
  font-style: italic;
  margin-left: 1rem;
`
