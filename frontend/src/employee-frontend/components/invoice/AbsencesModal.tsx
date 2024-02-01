// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import styled from 'styled-components'

import {
  Absence,
  AbsenceCategory,
  AbsenceType
} from 'lib-common/generated/api-types/absence'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import Title from 'lib-components/atoms/Title'
import Tooltip from 'lib-components/atoms/Tooltip'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { fontWeights } from 'lib-components/typography'
import { absenceTypes } from 'lib-customizations/employee'
import { faAbacus } from 'lib-icons'

import { getAbsencesByChild } from '../../api/invoicing'
import PeriodPicker from '../../components/absences/PeriodPicker'
import ColorInfoItem from '../../components/common/ColorInfoItem'
import { Lang, Translations, useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
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
  const { i18n, lang } = useTranslation()
  const { clearUiMode } = useContext(UIContext)
  const [selectedDate, setSelectedDate] = useState<LocalDate>(date)
  const [absences] = useApiState(
    () =>
      getAbsencesByChild(child.id, {
        year: selectedDate.getYear(),
        month: selectedDate.getMonth()
      }),
    [child.id, selectedDate]
  )

  return (
    <InfoModal
      data-qa="backup-care-group-modal"
      title={i18n.absences.modal.absenceSummaryTitle}
      icon={faAbacus}
      close={() => clearUiMode()}
      closeLabel={i18n.common.cancel}
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
              {absenceTypes.map((absenceType: AbsenceType) => (
                <tr key={absenceType}>
                  <TableData>
                    <ColorInfoItem type={absenceType} maxWidth={290} noMargin />
                  </TableData>
                  <TableData>
                    <Tooltip
                      tooltip={createTooltipText(
                        absences,
                        absenceType,
                        'NONBILLABLE',
                        lang
                      )}
                      position="left"
                    >
                      {calculateAbsences(
                        absences,
                        absenceType,
                        'NONBILLABLE',
                        i18n
                      )}
                    </Tooltip>
                  </TableData>
                  <TableData>
                    <Tooltip
                      tooltip={createTooltipText(
                        absences,
                        absenceType,
                        'BILLABLE',
                        lang
                      )}
                      position="right"
                    >
                      {calculateAbsences(
                        absences,
                        absenceType,
                        'BILLABLE',
                        i18n
                      )}
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
  category: AbsenceCategory,
  i18n: Translations
) {
  const absencesListSize = absences.filter(
    (abs: Absence) =>
      abs.absenceType === absenceType && abs.category === category
  ).length
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
  category: AbsenceCategory,
  lang: Lang
) {
  const absencesList = absences
    .filter(
      (abs: Absence) =>
        abs.absenceType === absenceType && abs.category === category
    )
    .map(({ date }) => date.format('EEEEEE dd.MM.yyyy', lang))
  return absencesList.join('<br />')
}
