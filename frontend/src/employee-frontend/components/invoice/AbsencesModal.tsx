// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import styled from 'styled-components'
import { Absence } from 'lib-common/api-types/child/Absences'
import { AbsenceType } from 'lib-common/generated/enums'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import Title from 'lib-components/atoms/Title'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { fontWeights } from 'lib-components/typography'
import { faAbacus } from 'lib-icons'
import { getAbsencesByChild } from '../../api/invoicing'
import PeriodPicker from '../../components/absences/PeriodPicker'
import ColorInfoItem from '../../components/common/ColorInfoItem'
import Tooltip from '../../components/common/Tooltip'
import { Translations, useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { AbsenceTypes, billableCareTypes } from '../../types/absence'
import { formatName } from '../../utils'
import { renderResult } from '../async-rendering'

const Section = styled.section``

const CustomTitle = styled(Title)``

const Table = styled.table`
  border-collapse: separate;
  border-spacing: 0 10px;
`

const FirstColumnTitle = styled.th`
  font-size: 14px;
  font-weight: ${fontWeights.bold};
  width: 320px;
`

const SecondColumnTitle = styled.th`
  font-size: 14px;
  font-weight: ${fontWeights.bold};
  width: 121px;
`

const ThirdColumnTitle = styled.th`
  font-size: 14px;
  font-weight: ${fontWeights.bold};
  width: 86px;
`

const TableData = styled.td`
  margin-bottom: 10px;
`

interface Props {
  child: { id: UUID; firstName: string; lastName: string }
  date: LocalDate
}

export default React.memo(function AbsencesModal({ child, date }: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode } = useContext(UIContext)
  const [selectedDate, setSelectedDate] = useState<LocalDate>(date)
  const [absences] = useApiState(
    () =>
      getAbsencesByChild(child.id, {
        year: selectedDate.getYear(),
        month: selectedDate.getMonth()
      }).then((res) =>
        res.map((obj) =>
          Object.values(obj)
            .filter((elem) => elem.length > 0)
            .flat()
        )
      ),
    [child.id, selectedDate]
  )

  return (
    <InfoModal
      data-qa="backup-care-group-modal"
      title={i18n.absences.modal.absenceSummaryTitle}
      icon={faAbacus}
      close={() => clearUiMode()}
    >
      {renderResult(absences, (absences) => (
        <Section>
          <CustomTitle size={4}>
            {formatName(child.firstName, child.lastName, i18n)}
          </CustomTitle>

          <PeriodPicker onChange={setSelectedDate} date={selectedDate} />

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
              {AbsenceTypes.map((absenceType: AbsenceType) => (
                <tr key={absenceType}>
                  <TableData>
                    <ColorInfoItem type={absenceType} maxWidth={290} noMargin />
                  </TableData>
                  <TableData>
                    <Tooltip
                      tooltipId={`tooltip_free-${absenceType}`}
                      tooltipText={createTooltipText(
                        absences,
                        absenceType,
                        'free',
                        i18n
                      )}
                      place="left"
                      className="absence-tooltip"
                      delayShow={1}
                    >
                      {calculateAbsences(absences, absenceType, 'free', i18n)}
                    </Tooltip>
                  </TableData>
                  <TableData>
                    <Tooltip
                      tooltipId={`tooltip_paid-${absenceType}`}
                      tooltipText={createTooltipText(
                        absences,
                        absenceType,
                        'paid',
                        i18n
                      )}
                      place="right"
                      className="absence-tooltip"
                      delayShow={1}
                    >
                      {calculateAbsences(absences, absenceType, 'paid', i18n)}
                    </Tooltip>
                  </TableData>
                </tr>
              ))}
            </tbody>
          </Table>
        </Section>
      ))}
    </InfoModal>
  )
})

function calculateAbsences(
  absences: Absence[],
  absenceType: AbsenceType,
  type: 'free' | 'paid',
  i18n: Translations
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

function createTooltipText(
  absences: Absence[],
  absenceType: AbsenceType,
  type: 'free' | 'paid',
  i18n: Translations
) {
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
          i18n.datePicker.weekdaysShort[abs.date.getIsoDayOfWeek() - 1]
        } ${abs.date.format()}`
    )
  return absencesList.join('<br />')
}
