// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import LocalDate from '@evaka/lib-common/src/local-date'
import { useTranslation } from '~state/i18n'
import { UIContext } from '~state/ui'
import InfoModal from '@evaka/lib-components/src/molecules/modals/InfoModal'
import Loader from '@evaka/lib-components/src/atoms/Loader'
import Title from '@evaka/lib-components/src/atoms/Title'
import { getDay } from 'date-fns'
import { formatName } from '~utils'
import { getAbsencesByChild } from '~api/invoicing'
import {
  Absence,
  AbsenceTypes,
  AbsenceType,
  billableCareTypes
} from '~types/absence'
import { UUID } from '~types'
import ColourInfoItem from '~components/common/ColourInfoItem'
import Tooltip from '~components/common/Tooltip'
import PeriodPicker, {
  PeriodPickerMode
} from '~components/absences/PeriodPicker'
import { faAbacus } from '@evaka/lib-icons'

const Section = styled.section``

const CustomTitle = styled(Title)``

const Table = styled.table`
  border-collapse: separate;
  border-spacing: 0 10px;
`

const FirstColumnTitle = styled.th`
  font-size: 14px;
  font-weight: bold;
  width: 320px;
`

const SecondColumnTitle = styled.th`
  font-size: 14px;
  font-weight: bold;
  width: 121px;
`

const ThirdColumnTitle = styled.th`
  font-size: 14px;
  font-weight: bold;
  width: 86px;
`

const TableData = styled.td`
  margin-bottom: 10px;
`

interface Props {
  child: { id: UUID; firstName: string; lastName: string }
  date: LocalDate
}

export default function AbsencesModal({ child, date }: Props) {
  const { i18n } = useTranslation()
  const [loading, setLoading] = useState<boolean>(true)
  const [absences, setAbsences] = useState<Absence[]>([])
  const { clearUiMode } = useContext(UIContext)
  const [selectedDate, setSelectedDate] = useState<LocalDate>(date)
  const [failure, setFailure] = useState<boolean>(false)

  useEffect(() => {
    void getAbsencesByChild(child.id, {
      year: selectedDate.getYear(),
      month: selectedDate.getMonth()
    }).then((res) => {
      if (res.isSuccess) {
        setAbsences(
          Object.values(res.value)
            .filter((elem: Absence[]) => elem.length > 0)
            .flat()
        )
        setFailure(false)
        setTimeout(() => {
          setLoading(false)
        }, 500)
      } else if (res.isFailure) {
        setFailure(true)
        setTimeout(() => {
          setLoading(false)
        }, 500)
      }
    })
  }, [selectedDate])

  function calculateAbsences(
    absences: Absence[],
    absenceType: AbsenceType,
    type: 'free' | 'paid'
  ) {
    const absencesListSize = absences
      .filter((abs: Absence) => abs.absenceType === absenceType)
      .filter((abs: Absence) => {
        return type === 'paid'
          ? billableCareTypes.includes(abs.careType)
          : !billableCareTypes.includes(abs.careType)
      }).length
    if (absencesListSize > 0) {
      return absencesListSize === 1
        ? `${absencesListSize} ${i18n.common.day}`
        : `${absencesListSize} ${i18n.common.days}`
    } else {
      return ``
    }
  }

  function createTooltipText(absenceType: AbsenceType, type: 'free' | 'paid') {
    const absencesList = absences
      .filter((abs: Absence) => abs.absenceType === absenceType)
      .filter((abs: Absence) => {
        return type === 'paid'
          ? billableCareTypes.includes(abs.careType)
          : !billableCareTypes.includes(abs.careType)
      })
      .map(
        (abs: Absence) =>
          `${
            i18n.datePicker.weekdaysShort[getDay(abs.date.toSystemTzDate())]
          } ${abs.date.format()}`
      )
    return absencesList.join('<br />')
  }

  return (
    <InfoModal
      data-qa="backup-care-group-modal"
      title={i18n.absences.modal.absenceSummaryTitle}
      icon={faAbacus}
      close={() => clearUiMode()}
      customSize={'650px'}
      size={'custom'}
    >
      {loading && <Loader />}
      {failure && <div>{i18n.common.loadingFailed}</div>}
      {!loading && child && (
        <Section>
          <CustomTitle size={4}>
            {formatName(child.firstName, child.lastName, i18n)}
          </CustomTitle>

          <PeriodPicker
            mode={PeriodPickerMode.MONTH}
            onChange={setSelectedDate}
            date={selectedDate}
          />

          <Table>
            <thead>
              <tr>
                <FirstColumnTitle>
                  {i18n.absences.modal.absenceSectionLabel}
                </FirstColumnTitle>
                <SecondColumnTitle>
                  {i18n.absences.modal.free}
                </SecondColumnTitle>
                <ThirdColumnTitle>{i18n.absences.modal.paid}</ThirdColumnTitle>
              </tr>
            </thead>
            <tbody>
              {AbsenceTypes.filter(
                (type: AbsenceType) => type !== 'PRESENCE'
              ).map((absenceType: AbsenceType) => (
                <tr key={absenceType}>
                  <TableData>
                    <ColourInfoItem
                      type={absenceType}
                      maxWidth={290}
                      noMargin
                    />
                  </TableData>
                  <TableData>
                    <Tooltip
                      tooltipId={`tooltip_free-${absenceType}`}
                      tooltipText={createTooltipText(absenceType, 'free')}
                      place={'left'}
                      className={'absence-tooltip'}
                      delayShow={1}
                    >
                      {calculateAbsences(absences, absenceType, 'free')}
                    </Tooltip>
                  </TableData>
                  <TableData>
                    <Tooltip
                      tooltipId={`tooltip_paid-${absenceType}`}
                      tooltipText={createTooltipText(absenceType, 'paid')}
                      place={'right'}
                      className={'absence-tooltip'}
                      delayShow={1}
                    >
                      {calculateAbsences(absences, absenceType, 'paid')}
                    </Tooltip>
                  </TableData>
                </tr>
              ))}
            </tbody>
          </Table>
        </Section>
      )}
    </InfoModal>
  )
}
