// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { StaticChip } from 'lib-components/atoms/Chip'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { H2 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { farUser } from 'lib-icons'

import { useTranslation } from '../../common/i18n'
import { IconBox } from '../StaffListItem'
import type { Staff } from '../utils'

function getColorByStatus(present: boolean) {
  return present ? colors.status.success : colors.accents.a6turquoise
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
  color: ${colors.grayscale.g35};
  top: 10px;
  position: relative;
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

          <H2 noMargin centered data-qa="employee-name">
            {name}
          </H2>

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
