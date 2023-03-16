// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { useLang, useTranslation } from 'citizen-frontend/localization'
import { BoundForm, useFormElems, useFormField } from 'lib-common/form/hooks'
import { scrollIntoViewSoftKeyboard } from 'lib-common/utils/scrolling'
import { fontWeights, Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import TimeInputs from './TimeInputs'
import { irregularDay, irregularTimes } from './form'

export interface IrregularRepetitionTimeInputGridProps {
  bind: BoundForm<typeof irregularTimes>
  childrenInShiftCare: boolean
  includedDays: number[]
  showAllErrors: boolean
}

export default React.memo(function IrregularRepetitionTimeInputGrid({
  bind,
  showAllErrors,
  childrenInShiftCare,
  includedDays
}: IrregularRepetitionTimeInputGridProps) {
  const irregularDays = useFormElems(bind)
  return (
    <>
      {irregularDays.map((irregularDay, index) => {
        const date = irregularDay.state.date
        return (
          <IrregularDay
            key={`shift-care-${date.formatIso()}`}
            bind={irregularDay}
            index={index}
            includedDays={includedDays}
            showAllErrors={showAllErrors}
            childrenInShiftCare={childrenInShiftCare}
          />
        )
      })}
    </>
  )
})

const IrregularDay = React.memo(function IrregularDay({
  bind,
  index,
  includedDays,
  showAllErrors,
  childrenInShiftCare
}: {
  bind: BoundForm<typeof irregularDay>
  index: number
  includedDays: number[]
  showAllErrors: boolean
  childrenInShiftCare: boolean
}) {
  const i18n = useTranslation()
  const [lang] = useLang()
  const date = bind.state.date

  const mode = useFormField(bind, 'mode')
  const day = useFormField(bind, 'day')
  const times = useFormField(day, 'times')
  const present = useFormField(day, 'present')

  return (
    <div data-qa={`time-input-${date.formatIso()}`}>
      {index !== 0 && date.getIsoDayOfWeek() === 1 ? <Separator /> : null}
      {index === 0 || date.getIsoDayOfWeek() === 1 ? (
        <Week>
          {i18n.common.datetime.week} {date.getIsoWeek()}
        </Week>
      ) : null}
      {includedDays.includes(date.getIsoDayOfWeek()) && (
        <TimeInputs
          label={<Label>{date.format('EEEEEE d.M.', lang)}</Label>}
          mode={mode.value()}
          bindPresent={present}
          bindTimes={times}
          showAllErrors={showAllErrors}
          allowExtraTimeRange={childrenInShiftCare}
          dataQaPrefix={`irregular-${date.formatIso()}`}
          onFocus={(ev) => {
            scrollIntoViewSoftKeyboard(ev.target)
          }}
        />
      )}
    </div>
  )
})

const Week = styled.div`
  color: ${(p) => p.theme.colors.main.m1};
  font-weight: ${fontWeights.semibold};
  grid-column-start: 1;
  grid-column-end: 4;
`

const Separator = styled.div`
  border-top: 2px dotted ${(p) => p.theme.colors.grayscale.g15};
  margin: ${defaultMargins.s} 0;
  grid-column-start: 1;
  grid-column-end: 4;
`
