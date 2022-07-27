// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useState } from 'react'
import styled from 'styled-components'

import { CustomTitle } from 'employee-mobile-frontend/components/attendances/components'
import type { StaffAttendanceType } from 'lib-common/generated/api-types/attendance'
import { ChipWrapper, ChoiceChip } from 'lib-components/atoms/Chip'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { Gap } from 'lib-components/white-space'
import type { Translations } from 'lib-customizations/employeeMobile'
import { fasInfo } from 'lib-icons'

interface Props {
  i18n: Translations
  types: StaffAttendanceType[]
  selectedType: StaffAttendanceType | undefined
  setSelectedType: (v: StaffAttendanceType | undefined) => void
}

export default React.memo(function StaffAttendanceTypeSelection({
  i18n,
  types,
  selectedType,
  setSelectedType
}: Props) {
  const [showInfo, setShowInfo] = useState(false)
  const toggleInfo = useCallback(() => setShowInfo((value) => !value), [])

  return (
    <Container>
      {showInfo && <InfoBox message={i18n.attendances.staff.differenceInfo} />}
      <InfoToggle onClick={toggleInfo}>
        <InfoBall>
          <FontAwesomeIcon icon={fasInfo} />
        </InfoBall>
        <Gap size="xs" horizontal />
        <InfoText>{i18n.attendances.staff.differenceInfoToggle}</InfoText>
      </InfoToggle>
      <Gap size="s" />
      <CustomTitle>{i18n.attendances.staff.differenceReason}</CustomTitle>
      <Gap size="s" />
      <ChipWrapper margin="xs" $justifyContent="center">
        {types.map((type) => (
          <ChoiceChip
            key={type}
            text={i18n.attendances.staffTypes[type]}
            selected={selectedType === type}
            onChange={() =>
              selectedType === type
                ? setSelectedType(undefined)
                : setSelectedType(type)
            }
          />
        ))}
      </ChipWrapper>
    </Container>
  )
})

const Container = styled.div`
  font-size: 1rem;
  color: ${({ theme }) => theme.colors.grayscale.g100};
  font-weight: 400;
`

const InfoToggle = styled.button`
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  background: transparent;
  color: inherit;
  border: none;
  outline: none;
  cursor: pointer;
`

const InfoBall = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  width: calc(1em + 6px);
  height: calc(1em + 6px);
  border-radius: 1em;
  background: ${({ theme }) => theme.colors.main.m2};
  color: ${({ theme }) => theme.colors.grayscale.g0};
`

const InfoText = styled.span`
  color: ${({ theme }) => theme.colors.grayscale.g70};
  font-size: 1rem;
`
