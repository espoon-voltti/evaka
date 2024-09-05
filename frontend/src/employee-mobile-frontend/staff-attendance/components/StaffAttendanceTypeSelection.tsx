// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { CustomTitle } from 'employee-mobile-frontend/common/components'
import { StaffAttendanceType } from 'lib-common/generated/api-types/attendance'
import { ChipWrapper, SelectionChip } from 'lib-components/atoms/Chip'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { Gap } from 'lib-components/white-space'
import { Translations } from 'lib-customizations/employeeMobile'

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
  return (
    <Container>
      <InfoBoxContainer>
        <InfoBox message={i18n.attendances.staff.differenceInfo} />
      </InfoBoxContainer>
      <Gap size="s" />
      <CustomTitle>{i18n.attendances.staff.differenceReason}</CustomTitle>
      <Gap size="s" />
      <ChipWrapper margin="xs" $justifyContent="center">
        {types.map((type) => (
          <SelectionChip
            key={type}
            text={i18n.attendances.staffTypes[type]}
            selected={selectedType === type}
            onChange={() =>
              selectedType === type
                ? setSelectedType(undefined)
                : setSelectedType(type)
            }
            data-qa={`attendance-type-${type}`}
            hideIcon
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

const InfoBoxContainer = styled.div`
  display: flex;
  justify-content: center;
`
