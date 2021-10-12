// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { StaticChip } from 'lib-components/atoms/Chip'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { farUser } from 'lib-icons'
import styled from 'styled-components'
import { fontWeights } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import { useTranslation } from '../../../state/i18n'
import { Staff } from '../staff'
import { IconBox } from '../StaffListItem'

function getColorByStatus(present: boolean) {
  return present ? colors.accents.green : colors.accents.water
}

function getBackgroundColorByStatus(present: boolean) {
  return present ? '#EEF4B3' : '#E2ECF2'
}

const Center = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 100vw;
`

const EmployeeStatus = styled.div`
  color: ${colors.greyscale.medium};
  top: 10px;
  position: relative;
`

const CustomTitle = styled.h2`
  font-family: Montserrat, 'Arial', sans-serif;
  font-style: normal;
  font-weight: ${fontWeights.semibold};
  font-size: 20px;
  line-height: 30px;
  margin-top: 0;
  color: ${colors.blues.dark};
  text-align: center;
  margin-bottom: ${defaultMargins.xs};
`

const Zindex = styled.div`
  z-index: 1;
  margin-left: -8%;
  margin-right: -8%;
`

const EmployeeBackground = styled.div<{ present: boolean }>`
  background: ${(p) => getBackgroundColorByStatus(p.present)};
  display: flex;
  flex-direction: column;
  align-items: center;
  border-radius: 0 0 50% 50%;
  padding-top: ${defaultMargins.s};
`

export function EmployeeCardBackground({
  staff: { name, present }
}: {
  staff: Staff
}) {
  const { i18n } = useTranslation()
  return (
    <Zindex>
      <EmployeeBackground present={present}>
        <Center>
          <IconBox present={present}>
            <RoundIcon
              content={farUser}
              color={getColorByStatus(present)}
              size="XXL"
            />
          </IconBox>

          <Gap size="s" />

          <CustomTitle data-qa="employee-name">{name}</CustomTitle>

          <EmployeeStatus>
            <StaticChip
              color={getColorByStatus(present)}
              data-qa="employee-status"
            >
              {present
                ? i18n.attendances.types.PRESENT
                : i18n.attendances.types.ABSENT}
            </StaticChip>
          </EmployeeStatus>
        </Center>
      </EmployeeBackground>
    </Zindex>
  )
}
