// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import LocalDate from '@evaka/lib-common/src/local-date'
import Title from '~components/shared/atoms/Title'
import LabelValueList from '~components/common/LabelValueList'
import { useTranslation } from '../../state/i18n'
import { formatDate } from '../../utils/date'
import {
  FeeDecisionPartDetailed,
  FeeDecisionStatus,
  FeeDecisionType,
  PersonDetailed
} from '../../types/invoicing'
import { getPdfUrl } from '../../api/invoicing'
import WarningLabel from '~components/common/WarningLabel'
import { EspooColours } from '~utils/colours'
import { formatName } from '~utils'
import Select from '~components/common/Select'

export const TitleRow = styled.div`
  display: flex;
  margin-bottom: 1.5rem;
  justify-content: space-between;
`

export const InfoMarkers = styled.span``

const DisabledLink = styled.span`
  color: ${EspooColours.grey};
`

const Cursive = styled.span`
  font-style: italic;
  margin-left: 1rem;
`

interface TypeSelectProps {
  selected: string
  changeDecisionType: (type: string) => void
}

const TypeSelect = ({ selected, changeDecisionType }: TypeSelectProps) => {
  const { i18n } = useTranslation()

  interface Option {
    value: string
    label: string
  }
  const decisionTypeOptions: Option[] = [
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

  const initialValue = decisionTypeOptions.find(
    (type) => type.value === selected
  )

  return (
    <Select
      placeholder={'Valitse...'}
      value={initialValue}
      options={decisionTypeOptions}
      fullWidth
      onChange={(value) =>
        value && 'value' in value ? changeDecisionType(value.value) : undefined
      }
    />
  )
}

interface Props {
  id: string
  status: FeeDecisionStatus
  headOfFamily: PersonDetailed
  decisionNumber: number | null
  validFrom: LocalDate
  validTo: LocalDate | null
  sentAt: Date | null
  documentKey: string | null
  parts: FeeDecisionPartDetailed[]
  decisionType: FeeDecisionType
  changeDecisionType: (type: string) => void
  newDecisionType: string
}

function displayDecisionNumber(number: number) {
  return number.toString().padStart(12, '0')
}

const Heading = React.memo(function Heading({
  id,
  status,
  headOfFamily,
  decisionNumber,
  validFrom,
  validTo,
  sentAt,
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
    <a href={getPdfUrl(id)} download>
      {i18n.feeDecision.downloadPdf}
    </a>
  ) : (
    <DisabledLink>
      {i18n.feeDecision.downloadPdf}
      <Cursive>{i18n.feeDecision.pdfInProgress}</Cursive>
    </DisabledLink>
  )

  function contents() {
    const decisionNUmberExists: {
      label: React.ReactNode
      value: React.ReactNode
      valueWidth?: string
      dataQa?: string
    }[] = decisionNumber
      ? [
          {
            label: i18n.feeDecision.decisionNUmber,
            value: displayDecisionNumber(decisionNumber)
          }
        ]
      : []

    const notDraft: {
      label: React.ReactNode
      value: React.ReactNode
      valueWidth?: string
      dataQa?: string
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
      dataQa?: string
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
        value: `${validFrom.format()} - ${validTo?.format() ?? ''}`
      },
      {
        label: i18n.feeDecision.sentAt,
        value: formatDate(sentAt)
      },
      {
        label: i18n.feeDecision.relief,
        value: reliefValue,
        valueWidth: '100%'
      },
      {
        label: i18n.feeDecision.sentAt,
        value: formatDate(sentAt)
      }
    ]

    return decisionNUmberExists.concat(notDraft).concat(always)
  }

  return (
    <>
      <TitleRow>
        <Title size={1}>{i18n.feeDecision.title[status]}</Title>
        <InfoMarkers>
          {headOfFamily.restrictedDetailsEnabled && (
            <WarningLabel text={i18n.childInformation.restrictedDetails} />
          )}
        </InfoMarkers>
      </TitleRow>
      <LabelValueList spacing="small" contents={contents()}></LabelValueList>
    </>
  )
})

export default Heading
