// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import { useDataStatus } from 'lib-common/utils/result-to-data-status'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Container from 'lib-components/layout/Container'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faTreePalm } from 'lib-icons'

import { UnwrapResult } from '../async-rendering'
import { useUser } from '../auth/state'
import { useTranslation } from '../localization'

import { useHolidayPeriods } from './state'

const BannerBackground = styled.div`
  background-color: ${colors.grayscale.g0};
`

const Banner = styled(Container)`
  display: flex;
  justify-content: center;
  align-items: center;
  gap: ${defaultMargins.s};
  padding: ${defaultMargins.s};
  margin-top: 1px;
`

interface HolidayPeriodBannerProps {
  period: FiniteDateRange
  reservationDeadline: LocalDate
}

const HolidayPeriodBanner = React.memo(function HolidayPeriodBanner({
  period,
  reservationDeadline
}: HolidayPeriodBannerProps) {
  const i18n = useTranslation()
  return (
    <Banner data-qa="holiday-period-banner">
      <RoundIcon content={faTreePalm} size="L" color={colors.status.warning} />
      <span>
        {i18n.ctaBanner.holidayPeriodCta(
          period.formatCompact(''),
          reservationDeadline.format()
        )}
      </span>
    </Banner>
  )
})

export default React.memo(function CtaBanner() {
  const user = useUser()
  const { holidayPeriods } = useHolidayPeriods()
  const status = useDataStatus(holidayPeriods)

  if (!user) return null

  return (
    <BannerBackground
      data-qa="holiday-period-banner-container"
      data-status={status}
    >
      <UnwrapResult
        result={holidayPeriods}
        loading={() => null}
        failure={() => null}
      >
        {([holidayPeriod]) =>
          holidayPeriod ? (
            <HolidayPeriodBanner
              period={holidayPeriod.period}
              reservationDeadline={holidayPeriod.reservationDeadline}
            />
          ) : null
        }
      </UnwrapResult>
    </BannerBackground>
  )
})
