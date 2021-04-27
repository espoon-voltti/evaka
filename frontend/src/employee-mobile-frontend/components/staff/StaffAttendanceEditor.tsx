import React, { useState } from 'react'
import styled from 'styled-components'
import { formatDecimal } from 'lib-common/utils/number'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import colors from 'lib-components/colors'
import { H2, H5 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import PlusMinus from './PlusMinus'
import { StaffAttendance } from 'lib-common/api-types/staffAttendances'
import { useTranslation } from 'employee-mobile-frontend/state/i18n'

export interface Props {
  staffAttendance: StaffAttendance
  realizedOccupancy: number | undefined
  isSaving: boolean
  onConfirm: (value: StaffAttendance) => void
}

export default function StaffAttendanceEditor({
  staffAttendance,
  realizedOccupancy,
  isSaving,
  onConfirm
}: Props) {
  const { i18n } = useTranslation()

  const initialStaff = staffAttendance.count ?? 0
  const initialStaffOther = staffAttendance.countOther ?? 0

  const [staff, setStaff] = useState(initialStaff)
  const [staffOther, setStaffOther] = useState(initialStaffOther)

  const changed = staff !== initialStaff || staffOther !== initialStaffOther

  function reset() {
    setStaff(initialStaff)
    setStaffOther(initialStaffOther)
  }

  return (
    <>
      <H2
        bold
        smaller
        primary
        // Hack to make it fit to the content area
        style={{ marginLeft: -8, marginRight: -8 }}
      >
        {i18n.staff.title}
      </H2>
      <Subtitle>{i18n.staff.daycareResponsible}</Subtitle>
      <PlusMinus
        value={staff}
        onMinus={dec(staff, setStaff)}
        onPlus={inc(staff, setStaff)}
        disabled={isSaving}
      />
      <Gap size="m" />
      <Subtitle>{i18n.staff.other}</Subtitle>
      <PlusMinus
        value={staffOther}
        onMinus={dec(staffOther, setStaffOther)}
        onPlus={inc(staffOther, setStaffOther)}
        disabled={isSaving}
      />
      <Gap size="m" />
      <FixedSpaceRow justifyContent="center">
        <Button
          text={i18n.common.cancel}
          onClick={reset}
          disabled={!changed || isSaving}
        />
        <Button
          text={i18n.common.confirm}
          primary
          disabled={!changed || isSaving}
          onClick={() =>
            onConfirm({
              ...staffAttendance,
              count: staff,
              countOther: staffOther
            })
          }
        />
      </FixedSpaceRow>
      <Gap size="m" />
      <FixedSpaceRow justifyContent="center">
        <H5>
          {i18n.staff.realizedOccupancy}{' '}
          {realizedOccupancy === undefined
            ? '-'
            : `${formatDecimal(realizedOccupancy)} %`}
        </H5>
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
