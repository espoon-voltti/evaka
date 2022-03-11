// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { useDataStatus } from 'lib-common/utils/result-to-data-status'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { desktopMin } from 'lib-components/breakpoints'
import Container from 'lib-components/layout/Container'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faTreePalm } from 'lib-icons'

import { UnwrapResult } from '../async-rendering'
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
  text: string
}

const HolidayPeriodBanner = React.memo(function HolidayPeriodBanner({
  text
}: HolidayPeriodBannerProps) {
  return (
    <Banner data-qa="holiday-period-banner">
      <RoundIcon content={faTreePalm} size="L" color={colors.status.warning} />
      <span>{text}</span>
    </Banner>
  )
})

export default React.memo(function BannerWrapper() {
  const { holidayBanner } = useHolidayPeriods()
  const status = useDataStatus(holidayBanner)
  const i18n = useTranslation()

  return (
    <BannerBackground
      data-qa="holiday-period-banner-container"
      data-status={status}
    >
      <UnwrapResult
        result={holidayBanner}
        loading={() => null}
        failure={() => null}
      >
        {(banner) => {
          switch (banner.type) {
            case 'holiday':
              return (
                <HolidayPeriodBanner
                  text={i18n.ctaBanner.holidayPeriodCta(
                    banner.period.formatCompact(''),
                    banner.deadline.format()
                  )}
                />
              )
            case 'questionnaire':
              return (
                <HolidayPeriodBanner
                  text={i18n.ctaBanner.fixedPeriodCta(banner.deadline.format())}
                />
              )
            default:
              return null
          }
        }}
      </UnwrapResult>
    </BannerBackground>
  )
})
