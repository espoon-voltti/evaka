// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import { OccupancyResponse } from 'lib-common/generated/api-types/occupancy'
import { formatPercentage } from 'lib-common/utils/number'
import Title from 'lib-components/atoms/Title'
import Tooltip from 'lib-components/atoms/Tooltip'
import { fontWeights } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import { faArrowDown, faArrowUp, faEye, faEyeSlash } from 'lib-icons'

import { useTranslation } from '../../../../state/i18n'

interface CardProps {
  color: string
  active: boolean
}

const Card = styled.div<CardProps>`
  cursor: pointer;
  border-left-style: solid;
  border-left-color: ${(props) =>
    props.active ? props.color : colors.grayscale.g15};
  border-left-width: 10px;
  width: 100%;
  margin: 10px;
  padding: 10px 10px 10px 25px;
  box-shadow: 0px 2px 4px 1px ${colors.grayscale.g15};
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
  font-weight: ${fontWeights.semibold};
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
      ? colors.main.m1
      : type == 'planned'
        ? colors.accents.a6turquoise
        : type == 'realized'
          ? colors.status.success
          : colors.grayscale.g15

  return (
    <Card color={color} active={active} onClick={() => onClick()}>
      <HeaderContainer>
        <Title size={4} noMargin>
          {i18n.unit.occupancy.subtitles[type]}
        </Title>
        <FontAwesomeIcon
          icon={active ? faEye : faEyeSlash}
          color={active ? colors.main.m1 : colors.grayscale.g15}
          size="lg"
        />
      </HeaderContainer>
      {data.min && data.max ? (
        <DataContainer>
          <Tooltip tooltip={data.min.period.start.format()} position="right">
            <DataMinMaxContainer>
              <IconContainer>
                <FontAwesomeIcon
                  icon={faArrowDown}
                  color={active ? color : colors.grayscale.g15}
                />
              </IconContainer>
              <Value data-qa={`occupancies-minimum-${type}`}>
                Min. {formatPercentage(data.min.percentage)}
              </Value>
            </DataMinMaxContainer>
          </Tooltip>
          <Tooltip tooltip={data.max.period.start.format()} position="right">
            <DataMinMaxContainer>
              <IconContainer>
                <FontAwesomeIcon
                  icon={faArrowUp}
                  color={active ? color : colors.grayscale.g15}
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
