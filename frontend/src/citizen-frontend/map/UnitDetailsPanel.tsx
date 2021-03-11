// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useState } from 'react'
import styled from 'styled-components'
import { addDays, isSaturday, isSunday } from 'date-fns'
import { Result } from '@evaka/lib-common/api'
import { useRestApi } from '@evaka/lib-common/utils/useRestApi'
import { PublicUnit } from '@evaka/lib-common/api-types/units/PublicUnit'
import { CareType } from '@evaka/lib-common/api-types/units/enums'
import { H2, Label } from '@evaka/lib-components/typography'
import { ContentArea } from '@evaka/lib-components/layout/Container'
import InlineButton from '@evaka/lib-components/atoms/buttons/InlineButton'
import ExternalLink from '@evaka/lib-components/atoms/ExternalLink'
import { Gap } from '@evaka/lib-components/white-space'
import { faArrowLeft } from '@evaka/lib-icons'
import { mapViewBreakpoint } from '../map/const'
import { useLang, useTranslation } from '../localization'
import { MapAddress } from '../map/MapView'
import { queryDistance } from '../map/api'
import { formatDistance } from '../map/distances'

type Props = {
  unit: PublicUnit
  onClose: () => void
  selectedAddress: MapAddress | null
}

export default React.memo(function UnitDetailsPanel({
  unit,
  onClose,
  selectedAddress
}: Props) {
  const t = useTranslation()
  const [lang] = useLang()

  const [distance, setDistance] = useState<Result<number> | null>(null)
  const loadDistance = useRestApi(queryDistance, setDistance)
  useEffect(() => {
    if (selectedAddress && unit.location) {
      loadDistance(selectedAddress.coordinates, unit.location)
    } else {
      setDistance(null)
    }
  }, [unit, selectedAddress])

  const formatCareType = (type: CareType) => {
    switch (type) {
      case 'CENTRE':
      case 'FAMILY':
      case 'GROUP_FAMILY':
        return t.map.careTypes.DAYCARE.toLowerCase()
      case 'PRESCHOOL':
        return t.common.unit.careTypes.PRESCHOOL.toLowerCase()
      case 'PREPARATORY_EDUCATION':
        return t.common.unit.careTypes.PREPARATORY_EDUCATION.toLowerCase()
      case 'CLUB':
        return t.common.unit.careTypes.CLUB.toLowerCase()
    }
  }
  const careTypes = unit.type
    .sort((a, b) => {
      if (a === 'CENTRE') return -1
      if (b === 'CENTRE') return 1
      if (a === 'PREPARATORY_EDUCATION') return 1
      if (b === 'PREPARATORY_EDUCATION') return -1
      return 0
    })
    .map(formatCareType)
    .join(', ')

  const capitalize = (s: string) => s.charAt(0).toUpperCase() + s.substr(1)

  const getRouteLink = useCallback(() => {
    if (!unit.location || !selectedAddress) return null

    const root = 'https://reittiopas.hsl.fi/reitti/'
    const start = encodeURIComponent(
      `${selectedAddress.streetAddress}, ${selectedAddress.postOffice}::${selectedAddress.coordinates.lat},${selectedAddress.coordinates.lon}`
    )
    const end = encodeURIComponent(
      `${unit.streetAddress}, ${unit.postOffice}::${unit.location.lat},${unit.location.lon}`
    )
    let arrival = addDays(new Date(), 1)
    if (isSaturday(arrival)) {
      arrival = addDays(arrival, 2)
    } else if (isSunday(arrival)) {
      arrival = addDays(arrival, 1)
    }
    arrival.setHours(9)
    arrival.setMinutes(0, 0, 0)
    const params = `?arriveBy=true&time=${
      arrival.getTime() / 1000
    }&locale=${lang}`
    return root + start + '/' + end + params
  }, [selectedAddress, unit, lang])

  const routeLink = getRouteLink()

  return (
    <Wrapper data-qa="map-unit-details">
      <Area opaque>
        <Gap size="s" />
        <InlineButton
          dataQa="map-unit-details-back"
          text={'Takaisin hakuun'}
          icon={faArrowLeft}
          onClick={onClose}
        />
        <Gap size="s" />
        <H2 data-qa="map-unit-details-name">{unit.name}</H2>

        {selectedAddress && distance && distance.isLoading ? null : (
          <>
            <Gap size="s" />
            <Label>{t.map.address}</Label>
            <Gap size="xs" />
            <div>
              {unit.streetAddress}, {unit.postalCode} {unit.postOffice}
            </div>
            {selectedAddress && distance && (
              <div>
                {distance.isSuccess ? (
                  <span>
                    {t.map.distanceWalking}: {formatDistance(distance.value)}
                  </span>
                ) : (
                  <br />
                )}
              </div>
            )}
            <Gap size="s" />
            <Label>{t.map.careTypePlural}</Label>
            <Gap size="xs" />
            <div>{careTypes}</div>
            <Gap size="s" />
            <Label>{t.map.language}</Label>
            <Gap size="xs" />
            <div>{capitalize(t.common.unit.languages[unit.language])}</div>
            <Gap size="s" />
            <Label>{t.map.providerType}</Label>
            <Gap size="xs" />
            <div>{t.common.unit.providerTypes[unit.providerType]}</div>
            {unit.providerType === 'PRIVATE_SERVICE_VOUCHER' && (
              <>
                <Gap size="xs" />
                <div>
                  <ExternalLink
                    text={t.common.unit.providerTypes.PRIVATE_SERVICE_VOUCHER}
                    href={t.map.serviceVoucherLink}
                  />
                </div>
              </>
            )}
            <Gap size="s" />
            <Label>{t.map.shiftCareTitle}</Label>
            <Gap size="xs" />
            <div>
              {unit.roundTheClock ? t.map.shiftCareYes : t.map.shiftCareNo}
            </div>
            {unit.url && (
              <>
                <Gap size="s" />

                <Label>{t.map.unitHomepage}</Label>
                <Gap size="xs" />
                <ExternalLink text={t.map.homepage} href={unit.url} newTab />
              </>
            )}
            {routeLink && (
              <>
                <Gap size="s" />

                <Label>{t.map.route}</Label>
                <Gap size="xs" />
                <ExternalLink
                  text={t.map.routePlanner}
                  href={routeLink}
                  newTab
                />
              </>
            )}
          </>
        )}
      </Area>
    </Wrapper>
  )
})

const Wrapper = styled.div`
  width: 400px;
  min-width: 300px;
  flex-grow: 1;
  flex-shrink: 1;

  display: flex;
  flex-direction: column;

  .mobile-tabs {
    display: none;
  }

  @media (max-width: ${mapViewBreakpoint}) {
    width: 100%;
  }
`

const Area = styled(ContentArea)`
  flex-grow: 1;
  box-sizing: border-box;
  width: 100%;
  padding-right: 20px;

  .mobile-tabs {
    display: none;
  }
  @media (max-width: ${mapViewBreakpoint}) {
    .mobile-tabs {
      display: block !important;
    }
  }
`
