// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import colors from 'lib-customizations/common'
import { useTranslation } from '../../../../state/i18n'
import Title from 'lib-components/atoms/Title'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faArrowDown, faArrowUp, faEye, faEyeSlash } from 'lib-icons'
import Tooltip from '../../../../components/common/Tooltip'
import { formatPercentage } from 'lib-common/utils/number'
import { OccupancyResponse } from '../../../../api/unit'

interface CardProps {
  color: string
  active: boolean
}

const Card = styled.div<CardProps>`
  cursor: pointer;
  border-left-style: solid;
  border-left-color: ${(props) =>
    props.active ? props.color : colors.greyscale.lighter};
  border-left-width: 10px;
  width: 100%;
  margin: 10px;
  padding: 10px 10px 10px 25px;
  box-shadow: 0px 2px 4px 1px ${colors.greyscale.lighter};
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
      ? colors.brandEspoo.espooBlue
      : type == 'planned'
      ? colors.accents.water
      : type == 'realized'
      ? colors.accents.green
      : colors.greyscale.lighter

  return (
    <Card color={color} active={active} onClick={() => onClick()}>
      <HeaderContainer>
        <Title size={4} noMargin>
          {i18n.unit.occupancy.subtitles[type]}
        </Title>
        <FontAwesomeIcon
          icon={active ? faEye : faEyeSlash}
          color={
            active ? colors.brandEspoo.espooBlue : colors.greyscale.lighter
          }
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
                  color={active ? color : colors.greyscale.lighter}
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
                  color={active ? color : colors.greyscale.lighter}
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
