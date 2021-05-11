// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useRef } from 'react'
import styled from 'styled-components'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-components/colors'
import leaflet from 'leaflet'
import { FooterContent } from '../Footer'
import {
  MapContainer,
  Marker,
  Popup,
  TileLayer,
  useMap,
  ZoomControl
} from 'react-leaflet'
import { PublicUnit } from 'lib-common/api-types/units/PublicUnit'
import markerUnit from './marker-unit.svg'
import markerUnitHighlight from './marker-unit-highlight.svg'
import markerAddress from './marker-address.svg'
import { formatDistance, UnitWithDistance } from '../map/distances'
import { useTranslation } from '../localization'
import { formatCareTypes } from './format'
import { MapAddress } from '../map/MapView'
import { mapViewBreakpoint } from '../map/const'
import { isAutomatedTest } from 'lib-common/utils/helpers'
import ExternalLink from 'lib-components/atoms/ExternalLink'
import { mapConfig } from 'lib-customizations/citizen'

import MarkerClusterGroup from 'react-leaflet-markercluster'
import 'leaflet.markercluster/dist/MarkerCluster.css'
import 'leaflet.markercluster/dist/MarkerCluster.Default.css'

export interface Props {
  units: (UnitWithDistance | PublicUnit)[]
  selectedUnit: PublicUnit | null
  selectedAddress: MapAddress | null
  setSelectedUnit: (u: PublicUnit | null) => void
}

export default React.memo(function MapBox(props: Props) {
  return (
    <Wrapper className="map-box">
      <Map
        center={mapConfig.center}
        zoom={mapConfig.initialZoom}
        zoomControl={false}
      >
        <MapContents {...props} />
        <ZoomControl position="bottomright" />
      </Map>
      <FooterWrapper>
        <FooterContent />
      </FooterWrapper>
    </Wrapper>
  )
})

const addressIcon = new leaflet.Icon({
  iconUrl: markerAddress,
  iconSize: [20, 38],
  iconAnchor: [10, 38],
  popupAnchor: [0, -18]
})

const unitIcon = new leaflet.Icon({
  iconUrl: markerUnit,
  iconSize: [30, 30],
  popupAnchor: [0, -18]
})

const unitHighlightIcon = new leaflet.Icon({
  iconUrl: markerUnitHighlight,
  iconSize: [30, 30],
  popupAnchor: [0, -18]
})

function MapContents({
  units,
  selectedUnit,
  selectedAddress,
  setSelectedUnit
}: Props) {
  const map = useMap()

  useEffect(() => {
    if (selectedAddress) {
      const { lat, lon } = selectedAddress.coordinates
      map.stop()
      map.flyTo([lat, lon], mapConfig.addressZoom, {
        animate: !isAutomatedTest
      })
    }
  }, [map, selectedAddress])

  useEffect(() => {
    if (selectedUnit && selectedUnit.location) {
      const { lat, lon } = selectedUnit.location
      map.stop()
      map.panTo([lat, lon], { animate: !isAutomatedTest })
    }
  }, [map, selectedUnit])

  return (
    <>
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        detectRetina={true}
      />
      {selectedAddress && <AddressMarker address={selectedAddress} />}
      <MarkerClusterGroup maxClusterRadius={25}>
        {units.map((unit) => (
          <UnitMarker
            key={unit.id}
            unit={unit}
            isSelected={selectedUnit?.id === unit.id}
            selectedUnit={selectedUnit}
            setSelectedUnit={setSelectedUnit}
          />
        ))}
      </MarkerClusterGroup>
    </>
  )
}

function AddressMarker({ address }: { address: MapAddress }) {
  const { lat, lon } = address.coordinates
  const markerRef = useRef<leaflet.Marker>(null)

  useEffect(() => {
    const element = markerRef.current?.getElement()
    if (element) {
      element.setAttribute('data-qa', 'map-marker-address')
    }
  }, [markerRef])

  return (
    <Marker
      title={address.streetAddress}
      position={[lat, lon]}
      icon={addressIcon}
      zIndexOffset={20}
      ref={markerRef}
    />
  )
}

function UnitMarker({
  unit,
  isSelected,
  selectedUnit,
  setSelectedUnit
}: {
  unit: UnitWithDistance | PublicUnit
  isSelected: boolean
  selectedUnit: PublicUnit | null
  setSelectedUnit: (u: PublicUnit | null) => void
}) {
  const t = useTranslation()
  const markerRef = useRef<leaflet.Marker>(null)

  if (unit.location?.lat == null || unit.location?.lon == null) return null
  const { lat, lon } = unit.location

  return (
    <Marker
      title={unit.name}
      position={[lat, lon]}
      icon={isSelected ? unitHighlightIcon : unitIcon}
      zIndexOffset={isSelected ? 10 : 0}
      ref={markerRef}
      eventHandlers={{
        click: () => {
          if (selectedUnit) {
            setSelectedUnit(unit)
          }
        }
      }}
    >
      <UnitPopup>
        <div data-qa={`map-popup-${unit.id}`}>
          <UnitName data-qa="map-popup-name">{unit.name}</UnitName>
          <div>{t.common.unit.providerTypes[unit.providerType]}</div>
          {unit.providerType === 'PRIVATE' &&
            unit.preschoolApplyPeriod === null &&
            unit.daycareApplyPeriod === null &&
            unit.clubApplyPeriod === null && (
              <div data-qa="map-popup-no-applying">{t.map.noApplying}</div>
            )}
          <UnitDetails>
            <UnitDetailsLeft>
              {unit.streetAddress}
              <br />
              {formatCareTypes(t, unit.type).join(', ')}
            </UnitDetailsLeft>
            {'drivingDistance' in unit && unit.drivingDistance !== null && (
              <UnitDetailsRight>
                {formatDistance(unit.drivingDistance)}
              </UnitDetailsRight>
            )}
          </UnitDetails>
          {unit.providerType === 'PRIVATE_SERVICE_VOUCHER' && (
            <>
              <Gap size="s" />
              <ExternalLink
                text={t.common.unit.providerTypes.PRIVATE_SERVICE_VOUCHER}
                href={t.map.serviceVoucherLink}
              />
            </>
          )}
          {unit.url && (
            <>
              <Gap size="xs" />
              <ExternalLink text={t.map.homepage} href={unit.url} newTab />
            </>
          )}
        </div>
      </UnitPopup>
    </Marker>
  )
}

const Wrapper = styled.div`
  position: relative;
  z-index: 0;
  width: 100%;
  height: 100%;
  flex-grow: 100;
  flex-shrink: 0;

  display: flex;
  flex-direction: column;
  align-items: stretch;

  min-height: 500px;

  @media (max-width: ${mapViewBreakpoint}) {
    height: calc(100vh - 120px);
    min-height: unset;
  }

  .leaflet-popup-content-wrapper {
    border-radius: 2px;
  }
`

const Map = styled(MapContainer)`
  flex-grow: 1;
  width: 100%;
`

const FooterWrapper = styled.div`
  position: absolute;
  left: 0;
  bottom: 0;
  width: fit-content;
  background-color: ${colors.greyscale.white};
  z-index: 999;
  margin-right: 250px;

  > div {
    margin: 0 ${defaultMargins.s} 0;
  }

  @media (max-width: 600px) {
    position: unset;
    width: 100%;
    display: flex;
    flex-wrap: wrap;
    justify-content: space-evenly;
    margin: 0;

    div:first-child {
      width: 100%;
      text-align: center;
    }
  }
`

const UnitPopup = styled(Popup)`
  font-family: 'Open Sans', sans-serif;
  font-size: 16px;
  line-height: 24px;

  .leaflet-popup-close-button {
    font-size: 24px !important;
    width: auto !important;
    height: auto !important;
    padding: 6px !important;
  }
`

const UnitName = styled.div`
  font-weight: 600;
  margin-right: 20px;
`

const UnitDetails = styled.div`
  display: flex;
  font-size: 14px;
  font-weight: 600;
  line-height: 21px;
  color: #6e6e6e;
`

const UnitDetailsLeft = styled.div`
  flex: 1 1 auto;
`

const UnitDetailsRight = styled.div`
  margin-left: 20px;
  flex: 0 1 auto;
  white-space: nowrap;
`
