/*
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
import React, { useState } from 'react'
import styled from 'styled-components'
import { formatDecimal } from 'lib-common/utils/number'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H2, H5 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import PlusMinus from './PlusMinus'
import { Result } from 'lib-common/api'
import { Translations, useTranslation } from '../../state/i18n'
import { StaffAttendanceUpdate } from 'lib-common/api-types/staffAttendances'
import { UUID } from 'lib-common/types'
import LocalDate from 'lib-common/local-date'
import { endOfYesterday } from 'date-fns'
import { DATE_FORMAT_TIME_ONLY, formatDate } from 'lib-common/date'
import colors from 'lib-customizations/common'

export interface Props {
  groupId: UUID | undefined
  date: LocalDate
  count: number
  countOther: number
  updated: Date | null
  realizedOccupancy: number | undefined
  onConfirm: (value: StaffAttendanceUpdate) => Promise<Result<void>>
}

export default function StaffAttendanceEditor({
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

  function reset() {
    setStaff(count)
    setStaffOther(countOther)
  }

  return (
    <>
      <H2
        bold
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
      {groupId && (
        <>
          <FixedSpaceRow
            justifyContent="center"
            style={{ marginLeft: -32, marginRight: -32 }}
          >
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
      <FixedSpaceRow justifyContent="center" marginBottom="s">
        <H5 noMargin data-qa="updated">
          {updatedTime(i18n, updated)}
        </H5>
      </FixedSpaceRow>
      <FixedSpaceRow justifyContent="center">
        <OccupancyHeading noMargin data-qa="realized-occupancy">
          {groupId
            ? i18n.staff.realizedGroupOccupancy
            : i18n.staff.realizedUnitOccupancy}{' '}
          {realizedOccupancy === undefined
            ? '-'
            : `${formatDecimal(realizedOccupancy)} %`}
        </OccupancyHeading>
      </FixedSpaceRow>
    </>
  )
}

const inc = (value: number, setValue: (newValue: number) => void) => (): void =>
  setValue(value + 0.5)

const dec = (value: number, setValue: (newValue: number) => void) => (): void =>
  value > 0 ? setValue(value - 0.5) : undefined

const Subtitle = styled.h2`
  font-style: normal;
  font-weight: 600;
  font-size: 16px;
  line-height: 24px;
  margin: 0;
  color: ${colors.greyscale.darkest};
  text-align: center;
`

const CancelButton = styled(Button)`
  width: 172px;
`
const ConfirmButton = styled(AsyncButton)`
  width: 172px;
`

const OccupancyHeading = styled(H5)`
  color: #000000;
`

function updatedTime(i18n: Translations, date: Date | null): string {
  if (!date) return i18n.staff.notUpdated
  if (date <= endOfYesterday()) {
    return `${i18n.staff.updated} ${formatDate(date)}`
  } else {
    return `${i18n.staff.updatedToday} ${formatDate(
      date,
      DATE_FORMAT_TIME_ONLY
    )}`
  }
}
