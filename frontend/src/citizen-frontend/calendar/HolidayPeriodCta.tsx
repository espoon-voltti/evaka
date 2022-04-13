// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import { useDataStatus } from 'lib-common/utils/result-to-data-status'
import Toast from 'lib-components/molecules/Toast'
import colors from 'lib-customizations/common'
import { faTreePalm } from 'lib-icons'

import { UnwrapResult } from '../async-rendering'
import { HolidayCta, NoCta, useHolidayPeriods } from '../holiday-periods/state'
import { useTranslation } from '../localization'

export default React.memo(function HolidayPeriodCta() {
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

  return (
    <div data-qa="holiday-period-cta-container" data-status={status}>
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
              onClose={closeCta}
              offsetTop="64px"
              offsetTopDesktop="180px"
            >
              <span data-qa="holiday-period-cta">{getCtaText(cta)}</span>
            </Toast>
          ) : null
        }
      </UnwrapResult>
    </div>
  )
})
