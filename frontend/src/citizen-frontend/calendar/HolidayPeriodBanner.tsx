// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import { useDataStatus } from 'lib-common/utils/result-to-data-status'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { desktopMin } from 'lib-components/breakpoints'
import Container from 'lib-components/layout/Container'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faTreePalm } from 'lib-icons'

import { UnwrapResult } from '../async-rendering'
import { useUser } from '../auth/state'
import { bannerHeightDesktop } from '../header/const'
import { useHolidayPeriods } from '../holiday-periods/state'
import { useTranslation } from '../localization'

const BannerBackground = styled.div`
  position: sticky;
  z-index: 2;
  top: 0;
  background-color: ${colors.grayscale.g0};
`

const Banner = styled(Container)`
  display: flex;
  justify-content: center;
  align-items: center;
  gap: ${defaultMargins.s};
  padding: ${defaultMargins.s};

  @media (min-width: ${desktopMin}) {
    height: ${bannerHeightDesktop}px;
  }
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

export default React.memo(function BannerWrapper() {
  const user = useUser()
  const { activeFixedPeriodQuestionnaire } = useHolidayPeriods()
  const status = useDataStatus(activeFixedPeriodQuestionnaire)

  if (!user) {
    return null
  }

  return (
    <BannerBackground
      data-qa="holiday-period-banner-container"
      data-status={status}
    >
      <UnwrapResult
        result={activeFixedPeriodQuestionnaire}
        loading={() => null}
        failure={() => null}
      >
        {(activePeriod) => {
          return activePeriod ? (
            <HolidayPeriodBanner
              period={activePeriod.period}
              reservationDeadline={activePeriod.active.end}
            />
          ) : null
        }}
      </UnwrapResult>
    </BannerBackground>
  )
})
