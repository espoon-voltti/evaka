// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { PublicUnit } from 'lib-common/generated/api-types/daycare'
import { StaticChip } from 'lib-components/atoms/Chip'
import ExternalLink from 'lib-components/atoms/ExternalLink'
import { IconButton } from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { tabletMin } from 'lib-components/breakpoints'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap
} from 'lib-components/layout/flex-helpers'
import { BigNumber, H4 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faArrowDown, faArrowUp, faTimes } from 'lib-icons'

import { useTranslation } from '../../../localization'

export type PreferredUnitBoxProps = {
  unit: PublicUnit
  n: number
  remove: () => void
  moveUp: (() => void) | null
  moveDown: (() => void) | null
}

export default React.memo(function PreferredUnitBox({
  unit,
  n,
  remove,
  moveUp,
  moveDown
}: PreferredUnitBoxProps) {
  const t = useTranslation()

  const providerTypeText =
    unit.providerType === 'PRIVATE_SERVICE_VOUCHER'
      ? t.common.unit.providerTypes.PRIVATE
      : t.common.unit.providerTypes[unit.providerType]

  const getProviderTypeColors = (): { color: string } => {
    switch (unit.providerType) {
      case 'MUNICIPAL':
      case 'MUNICIPAL_SCHOOL':
        return { color: colors.accents.a6turquoise }
      case 'PRIVATE':
      case 'PRIVATE_SERVICE_VOUCHER':
        return { color: colors.accents.a3emerald }
      case 'PURCHASED':
      case 'EXTERNAL_PURCHASED':
        return { color: colors.main.m4 }
    }
  }

  return (
    <Wrapper data-qa={`preferred-unit-${unit.id}`}>
      <MainColLeft>
        <BigNumber>{n}</BigNumber>
      </MainColLeft>
      <MainColCenter>
        <FixedSpaceColumn>
          <FixedSpaceColumn spacing="xxs">
            <H4 noMargin>{unit.name}</H4>
            <span>{unit.streetAddress}</span>
            {unit.providerType === 'PRIVATE_SERVICE_VOUCHER' && (
              <ExternalLink
                href={
                  t.applications.editor.unitPreference.units.serviceVoucherLink
                }
                text={t.common.unit.providerTypes.PRIVATE_SERVICE_VOUCHER}
                newTab
              />
            )}
          </FixedSpaceColumn>
          <FixedSpaceFlexWrap horizontalSpacing="xs" verticalSpacing="xs">
            {unit.language === 'sv' ? (
              <StaticChip color={colors.accents.a5orangeLight}>
                {t.applications.editor.unitPreference.units.preferences.sv}
              </StaticChip>
            ) : (
              <StaticChip color={colors.main.m3}>
                {t.applications.editor.unitPreference.units.preferences.fi}
              </StaticChip>
            )}
            <StaticChip {...getProviderTypeColors()}>
              {providerTypeText.toLowerCase()}
            </StaticChip>
          </FixedSpaceFlexWrap>
          <Gap size="xs" />
          <FixedSpaceFlexWrap verticalSpacing="xs">
            <InlineButton
              text={
                t.applications.editor.unitPreference.units.preferences.moveUp
              }
              altText={`${unit.name} (${
                t.applications.editor.unitPreference.units.preferences[
                  unit.language
                ]
              }, ${providerTypeText}): ${
                t.applications.editor.unitPreference.units.preferences.moveUp
              }`}
              icon={faArrowUp}
              onClick={moveUp || noOp}
              disabled={!moveUp}
            />
            <InlineButton
              text={
                t.applications.editor.unitPreference.units.preferences.moveDown
              }
              altText={`${unit.name} (${
                t.applications.editor.unitPreference.units.preferences[
                  unit.language
                ]
              }, ${providerTypeText}): ${
                t.applications.editor.unitPreference.units.preferences.moveDown
              }`}
              icon={faArrowDown}
              onClick={moveDown || noOp}
              disabled={!moveDown}
            />
          </FixedSpaceFlexWrap>
        </FixedSpaceColumn>
      </MainColCenter>
      <MainColRight>
        <IconButton
          icon={faTimes}
          color="gray"
          onClick={remove}
          aria-label={`${unit.name}: ${t.applications.editor.unitPreference.units.preferences.remove}`}
        />
      </MainColRight>
    </Wrapper>
  )
})

const noOp = () => undefined

const Wrapper = styled.div`
  display: flex;
  flex-direction: row;
  border: 1px solid ${colors.main.m2};
  box-sizing: border-box;
  box-shadow: 0 4px 4px rgba(0, 0, 0, 0.15);
  border-radius: 2px;
`

const MainColLeft = styled.div`
  flex: 0 0 52px;
  text-align: center;
  padding: ${defaultMargins.xs};

  @media screen and (min-width: ${tabletMin}) {
    flex-basis: 64px;
    padding: ${defaultMargins.s};
  }
`

const MainColCenter = styled.div`
  flex-grow: 1;
  padding: ${defaultMargins.s} 0;
`

const MainColRight = styled.div`
  flex-grow: 0;
  padding: ${defaultMargins.s};
`
