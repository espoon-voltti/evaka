// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import { StaffAttendanceUpdate } from 'lib-common/generated/api-types/daycare'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'
import { formatDecimal } from 'lib-common/utils/number'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fontWeights, H2, H4 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { Translations, useTranslation } from '../common/i18n'

import PlusMinus from './PlusMinus'

export interface Props {
  groupId: UUID | undefined
  date: LocalDate
  count: number
  countOther: number
  updated: HelsinkiDateTime | null
  realizedOccupancy: number | undefined
  onConfirm: (value: StaffAttendanceUpdate) => Promise<Result<void>>
}

export default React.memo(function StaffAttendanceEditor({
  groupId,
  date,
  count,
  countOther,
  updated,
  realizedOccupancy,
  onConfirm
}: Props) {
  const { i18n } = useTranslation()

  const [saving, setSaving] = useState(false)
  const [staff, setStaff] = useState(count)
  const [staffOther, setStaffOther] = useState(countOther)

  const editable = groupId !== undefined
  const changed = staff !== count || staffOther !== countOther

  const reset = useCallback(() => {
    setStaff(count)
    setStaffOther(countOther)
  }, [count, countOther])

  return (
    <>
      <H2
        centered
        smaller
        primary
        // Hack to make it fit to the content area
        style={{ marginLeft: -8, marginRight: -8 }}
      >
        {i18n.staff.title}
      </H2>
      <Subtitle>{i18n.staff.daycareResponsible}</Subtitle>
      <PlusMinus
        editable={editable}
        value={staff}
        onMinus={dec(staff, setStaff)}
        onPlus={inc(staff, setStaff)}
        disabled={saving}
        data-qa="staff-count"
      />
      <Gap size="s" />
      <Subtitle>{i18n.staff.other}</Subtitle>
      <PlusMinus
        editable={editable}
        value={staffOther}
        onMinus={dec(staffOther, setStaffOther)}
        onPlus={inc(staffOther, setStaffOther)}
        disabled={saving}
        data-qa="staff-other-count"
      />
      <Gap size="s" />
      {!!groupId && (
        <>
          <FixedSpaceRow justifyContent="center">
            <CancelButton
              text={i18n.staff.cancel}
              onClick={reset}
              disabled={!changed || saving}
              data-qa="cancel-button"
            />
            <ConfirmButton
              text={i18n.common.confirm}
              primary
              disabled={!changed || saving}
              onClick={() => {
                setSaving(true)
                return onConfirm({
                  groupId,
                  date,
                  count: staff,
                  countOther: staffOther
                })
              }}
              onSuccess={() => {
                setSaving(false)
              }}
              data-qa="confirm-button"
            />
          </FixedSpaceRow>
          <Gap size="m" />
        </>
      )}
      <FixedSpaceRow justifyContent="center">
        <H4 noMargin data-qa="updated">
          {updatedTime(i18n, updated)}
        </H4>
      </FixedSpaceRow>
      <Gap size="s" />
      <FixedSpaceRow justifyContent="center">
        <H4 noMargin data-qa="realized-occupancy">
          {groupId
            ? i18n.staff.realizedGroupOccupancy
            : i18n.staff.realizedUnitOccupancy}{' '}
          {realizedOccupancy === undefined
            ? '-'
            : `${formatDecimal(realizedOccupancy)} %`}
        </H4>
      </FixedSpaceRow>
    </>
  )
})

const inc = (value: number, setValue: (newValue: number) => void) => (): void =>
  setValue(value + 0.5)

const dec =
  (value: number, setValue: (newValue: number) => void) => (): void =>
    value > 0 ? setValue(value - 0.5) : undefined

const Subtitle = styled.h2`
  font-style: normal;
  font-weight: ${fontWeights.semibold};
  font-size: 16px;
  line-height: 24px;
  margin: 0;
  color: ${colors.grayscale.g100};
  text-align: center;
`

const CancelButton = styled(LegacyButton)`
  width: 172px;
  padding: 0;
`
const ConfirmButton = styled(AsyncButton)`
  width: 172px;
  padding: 0;
`

function updatedTime(
  i18n: Translations,
  date: HelsinkiDateTime | null
): string {
  if (!date) return i18n.staff.notUpdated
  const yesterday = HelsinkiDateTime.now().toLocalDate().subDays(1)
  const endOfYesterday = HelsinkiDateTime.fromLocal(yesterday, LocalTime.MAX)
  if (date.isEqualOrBefore(endOfYesterday)) {
    return `${i18n.staff.updated} ${date.toLocalDate().format()}`
  } else {
    return `${i18n.staff.updatedToday} ${date.toLocalTime().format()}`
  }
}
