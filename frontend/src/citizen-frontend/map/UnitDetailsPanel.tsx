// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { addDays, isSaturday, isSunday } from 'date-fns'
import React, { useCallback } from 'react'
import styled from 'styled-components'

import { CareType, PublicUnit } from 'lib-common/generated/api-types/daycare'
import { useQueryResult } from 'lib-common/query'
import { capitalizeFirstLetter } from 'lib-common/string'
import { mockNow } from 'lib-common/utils/helpers'
import ExternalLink from 'lib-components/atoms/ExternalLink'
import { Button } from 'lib-components/atoms/buttons/Button'
import { ContentArea } from 'lib-components/layout/Container'
import { H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { routeLinkRootUrl } from 'lib-customizations/citizen'
import { faArrowLeft } from 'lib-icons'

import { useLang, useTranslation } from '../localization'

import { MapAddress } from './MapView'
import { mapViewBreakpoint } from './const'
import { formatDistance } from './distances'
import { distanceQuery } from './queries'

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

  const distance = useQueryResult(
    distanceQuery(selectedAddress?.coordinates ?? null, unit.location)
  )

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

  const getRouteLink = useCallback(() => {
    if (!unit.location || !selectedAddress || !routeLinkRootUrl) return null

    const start = encodeURIComponent(
      `${selectedAddress.streetAddress}, ${selectedAddress.postOffice}::${selectedAddress.coordinates.lat},${selectedAddress.coordinates.lon}`
    )
    const end = encodeURIComponent(
      `${unit.streetAddress}, ${unit.postOffice}::${unit.location.lat},${unit.location.lon}`
    )
    let arrival = addDays(mockNow() ?? new Date(), 1)
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
    return routeLinkRootUrl + start + '/' + end + params
  }, [selectedAddress, unit, lang])

  const routeLink = getRouteLink()

  return (
    <Wrapper data-qa="map-unit-details">
      <Area opaque>
        <Gap size="s" />
        <Button
          appearance="inline"
          data-qa="map-unit-details-back"
          text={t.map.backToSearch}
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
            {/* eslint-disable-next-line @typescript-eslint/no-unsafe-argument */}
            <div>
              {capitalizeFirstLetter(t.common.unit.languages[unit.language])}
            </div>
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
                    newTab
                  />
                </div>
              </>
            )}
            <Gap size="s" />
            <Label>{t.map.shiftCareTitle}</Label>
            <Gap size="xs" />
            <div>
              {unit.providesShiftCare ? t.map.shiftCareYes : t.map.shiftCareNo}
            </div>
            {!!unit.url && (
              <>
                <Gap size="s" />

                <Label>{t.map.unitHomepage}</Label>
                <Gap size="xs" />
                <ExternalLink text={t.map.homepage} href={unit.url} newTab />
              </>
            )}
            {!!routeLink && (
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

  @media (max-width: ${mapViewBreakpoint}) {
    width: 100%;
  }
`

const Area = styled(ContentArea)`
  flex-grow: 1;
  box-sizing: border-box;
  width: 100%;
`
