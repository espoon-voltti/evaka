// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import styled from 'styled-components'

import { useDataStatus } from 'lib-common/utils/result-to-data-status'
import Toast from 'lib-components/molecules/Toast'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faTreePalm } from 'lib-icons'

import { UnwrapResult } from '../async-rendering'
import { HolidayCta, NoCta, useHolidayPeriods } from '../holiday-periods/state'
import { useTranslation } from '../localization'

export default React.memo(function HolidayPeriodCta({
  openModal,
  topAligned
}: {
  openModal: () => void
  topAligned?: boolean
}) {
  const { holidayCta, ctaClosed, closeCta } = useHolidayPeriods()
  const status = useDataStatus(holidayCta)
  const i18n = useTranslation()

  const getCtaText = useCallback(
    (cta: Exclude<HolidayCta, NoCta>) => {
      switch (cta.type) {
        case 'holiday':
          return i18n.ctaToast.holidayPeriodCta(
            cta.period.formatCompact(''),
            cta.deadline.format()
          )
        case 'questionnaire':
          return i18n.ctaToast.fixedPeriodCta(cta.deadline.format())
      }
    },
    [i18n]
  )

  const clickAction = useCallback(() => {
    closeCta()
    openModal()
  }, [closeCta, openModal])

  return (
    <CtaToastContainer
      data-qa="holiday-period-cta-container"
      data-status={status}
      topAligned={topAligned}
    >
      <UnwrapResult
        result={holidayCta}
        loading={() => null}
        failure={() => null}
      >
        {(cta) =>
          cta.type !== 'none' && !ctaClosed ? (
            <Toast
              icon={faTreePalm}
              iconColor={colors.status.warning}
              onClick={cta.type === 'questionnaire' ? clickAction : undefined}
              onClose={closeCta}
            >
              <span data-qa="holiday-period-cta">{getCtaText(cta)}</span>
            </Toast>
          ) : null
        }
      </UnwrapResult>
    </CtaToastContainer>
  )
})

const CtaToastContainer = styled.div<{ topAligned?: boolean }>`
  position: absolute;
  top: ${(p) =>
    p.topAligned ? defaultMargins.s : `calc(100% + ${defaultMargins.s})`};
  right: ${defaultMargins.s};
  z-index: 100;
  padding-left: 16px;
`
