// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { EspooColours } from '~utils/colours'
import { useTranslation } from '~state/i18n'
import Title from '~components/shared/atoms/Title'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faArrowDown, faArrowUp, faEye, faEyeSlash } from 'icon-set'
import Tooltip from '~components/common/Tooltip'
import { formatPercentage } from '~components/utils'
import { OccupancyResponse } from '~api/unit'

interface CardProps {
  color: string
  active: boolean
}

const Card = styled.div<CardProps>`
  cursor: pointer;
  border-left-style: solid;
  border-left-color: ${(props) =>
    props.active ? props.color : EspooColours.greyLight};
  border-left-width: 10px;
  width: 100%;
  margin: 10px;
  padding: 10px 10px 10px 25px;
  box-shadow: 0px 2px 4px 1px ${EspooColours.greyLight};
`

const HeaderContainer = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;

  > .title {
    margin-bottom: 0;
  }
`

const DataContainer = styled.div`
  display: flex;
  justify-content: left;
`

const DataMinMaxContainer = styled.div`
  display: flex;
  margin-right: 25px;
`

const IconContainer = styled.div`
  margin-right: 10px;
`

const Value = styled.div`
  font-weight: 600;
  font-size: 1.2rem;
  white-space: nowrap;
`

interface Props {
  type: 'confirmed' | 'planned' | 'realized'
  data: OccupancyResponse
  active: boolean
  onClick: () => undefined | void
}

export default React.memo(function OccupancyCard({
  type,
  data,
  active,
  onClick
}: Props) {
  const { i18n } = useTranslation()

  const color =
    type == 'confirmed'
      ? EspooColours.espooBlue
      : type == 'planned'
      ? EspooColours.water
      : type == 'realized'
      ? EspooColours.green
      : EspooColours.greyLight

  return (
    <Card color={color} active={active} onClick={() => onClick()}>
      <HeaderContainer>
        <Title size={4} noMargin>
          {i18n.unit.occupancy.subtitles[type]}
        </Title>
        <FontAwesomeIcon
          icon={active ? faEye : faEyeSlash}
          color={active ? EspooColours.espooBlue : EspooColours.greyLight}
          size="lg"
        />
      </HeaderContainer>
      {data.min && data.max ? (
        <DataContainer>
          <Tooltip
            tooltipId={`occupancy-${type}-min-date`}
            tooltipText={data.min.period.start.format()}
            place={'right'}
          >
            <DataMinMaxContainer>
              <IconContainer>
                <FontAwesomeIcon
                  icon={faArrowDown}
                  color={active ? color : EspooColours.greyLight}
                />
              </IconContainer>
              <Value data-qa={`occupancies-minimum-${type}`}>
                Min. {formatPercentage(data.min.percentage)}
              </Value>
            </DataMinMaxContainer>
          </Tooltip>
          <Tooltip
            tooltipId={`occupancy-${type}-max-date`}
            tooltipText={data.max.period.start.format()}
            place={'right'}
          >
            <DataMinMaxContainer>
              <IconContainer>
                <FontAwesomeIcon
                  icon={faArrowUp}
                  color={active ? color : EspooColours.greyLight}
                />
              </IconContainer>
              <Value data-qa={`occupancies-maximum-${type}`}>
                Max. {formatPercentage(data.max.percentage)}
              </Value>
            </DataMinMaxContainer>
          </Tooltip>
        </DataContainer>
      ) : (
        <div data-qa={`occupancies-no-valid-values-${type}`}>
          {type == 'realized'
            ? i18n.unit.occupancy.noValidValuesRealized
            : i18n.unit.occupancy.noValidValues}
        </div>
      )}
    </Card>
  )
})
