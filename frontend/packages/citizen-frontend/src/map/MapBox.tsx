import React, { useEffect, useMemo } from 'react'
import styled from 'styled-components'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import colors from '@evaka/lib-components/src/colors'
import leaflet from 'leaflet'
import { FooterContent } from '~Footer'
import { MapContainer, Marker, Popup, TileLayer, useMap } from 'react-leaflet'
import { PublicUnit } from '@evaka/lib-common/src/api-types/units/PublicUnit'
import marker from './marker.svg'
import markerHighlight from './marker-highlight.svg'
import { formatDistance, UnitWithDistance } from '~map/distances'
import { useTranslation } from '~localization'
import { formatCareTypes } from './format'
import { MapAddress } from '~map/MapView'

export interface Props {
  units: (UnitWithDistance | PublicUnit)[]
  selectedUnit: PublicUnit | null
  selectedAddress: MapAddress | null
}

const initialZoom = 12
const addressZoom = 14

export default React.memo(function MapBox(props: Props) {
  return (
    <Wrapper className="map-box">
      <Map center={[60.184147, 24.704897]} zoom={initialZoom}>
        <MapContents {...props} />
      </Map>
      <FooterWrapper>
        <FooterContent />
      </FooterWrapper>
    </Wrapper>
  )
})

function MapContents({ units, selectedUnit, selectedAddress }: Props) {
  const map = useMap()
  const icon = useMemo(
    () =>
      new leaflet.Icon({
        iconUrl: marker,
        iconSize: [30, 30],
        popupAnchor: [0, -18]
      }),
    [marker]
  )
  const highlightIcon = useMemo(
    () =>
      new leaflet.Icon({
        iconUrl: markerHighlight,
        iconSize: [30, 30],
        popupAnchor: [0, -18]
      }),
    [markerHighlight]
  )
  const t = useTranslation()

  useEffect(() => {
    if (selectedAddress) {
      const { lat, lon } = selectedAddress.coordinates
      map.stop()
      map.flyTo([lat, lon], addressZoom, { animate: true })
    }
  }, [selectedAddress])

  useEffect(() => {
    if (selectedUnit && selectedUnit.location) {
      const { lat, lon } = selectedUnit.location
      map.stop()
      map.panTo([lat, lon], { animate: true })
    }
  }, [selectedUnit])

  return (
    <>
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        detectRetina={true}
      />
      {units.map((unit) => {
        const { location } = unit
        if (location?.lat == null || location?.lon == null) return null
        const isSelected = selectedUnit?.id === unit.id

        return (
          <Marker
            key={unit.id}
            title={unit.name}
            position={[location.lat, location.lon]}
            icon={isSelected ? highlightIcon : icon}
            zIndexOffset={isSelected ? 10 : 0}
          >
            <UnitPopup>
              <UnitName>{unit.name}</UnitName>
              <div>{t.common.unit.providerTypes[unit.providerType]}</div>
              <UnitDetails>
                <UnitDetailsLeft>
                  {unit.streetAddress}
                  <br />
                  {formatCareTypes(t, unit.type).join(', ')}
                </UnitDetailsLeft>
                {'straightDistance' in unit && (
                  <UnitDetailsRight>
                    {formatDistance(
                      unit.drivingDistance ?? unit.straightDistance
                    )}
                  </UnitDetailsRight>
                )}
              </UnitDetails>
            </UnitPopup>
          </Marker>
        )
      })}
    </>
  )
}

const Wrapper = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
  flex-grow: 100;
  flex-shrink: 0;

  display: flex;
  flex-direction: column;
  align-items: stretch;
`

const Map = styled(MapContainer)`
  flex-grow: 1;
  width: 100%;
  min-height: 500px;
`

const FooterWrapper = styled.div`
  position: absolute;
  left: 0;
  bottom: 0;
  width: fit-content;
  background-color: ${colors.greyscale.white};
  z-index: 999;

  > div {
    margin: 0 ${defaultMargins.s} 0;
  }

  @media (max-width: 600px) {
    position: unset;
    width: 100%;
    display: flex;
    flex-wrap: wrap;
    justify-content: space-evenly;

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
