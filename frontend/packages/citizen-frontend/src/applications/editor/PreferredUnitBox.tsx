import React from 'react'
import { PublicUnit } from '@evaka/lib-common/src/api-types/units'
import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap
} from '@evaka/lib-components/src/layout/flex-helpers'
import { H4 } from '@evaka/lib-components/src/typography'
import IconButton from '@evaka/lib-components/src/atoms/buttons/IconButton'
import { faArrowDown, faArrowUp, faTimes } from '@evaka/lib-icons'
import { defaultMargins, Gap } from '@evaka/lib-components/src/white-space'
import InlineButton from '@evaka/lib-components/src/atoms/buttons/InlineButton'
import { StaticChip } from '@evaka/lib-components/src/atoms/Chip'

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
  return (
    <Wrapper>
      <MainColLeft>{n}</MainColLeft>
      <MainColCenter>
        <FixedSpaceColumn>
          <FixedSpaceColumn spacing={'xxs'}>
            <H4 noMargin>{unit.name}</H4>
            <span>{unit.streetAddress}</span>
          </FixedSpaceColumn>
          <FixedSpaceFlexWrap horizontalSpacing={'xs'} verticalSpacing={'xs'}>
            <StaticChip color={colors.primary}>Suomenkielinen</StaticChip>
            <StaticChip color={colors.accents.water}>Kunnallinen</StaticChip>
          </FixedSpaceFlexWrap>
          <Gap size={'xs'} />
          <FixedSpaceFlexWrap verticalSpacing={'xs'}>
            <InlineButton
              text={'Siirrä ylöspäin'}
              icon={faArrowUp}
              onClick={moveUp || noOp}
              disabled={!moveUp}
            />
            <InlineButton
              text={'Siirrä alaspäin'}
              icon={faArrowDown}
              onClick={moveDown || noOp}
              disabled={!moveDown}
            />
          </FixedSpaceFlexWrap>
        </FixedSpaceColumn>
      </MainColCenter>
      <MainColRight>
        <IconButton icon={faTimes} gray onClick={remove} />
      </MainColRight>
    </Wrapper>
  )
})

const noOp = () => undefined

const Wrapper = styled.div`
  display: flex;
  flex-direction: row;
  border: 1px solid ${colors.primary};
  box-sizing: border-box;
  box-shadow: 0 4px 4px rgba(0, 0, 0, 0.15);
  border-radius: 2px;
`

const MainColLeft = styled.div`
  flex-grow: 0;
  font-family: Montserrat, sans-serif;
  font-style: normal;
  font-weight: 300;
  color: ${colors.primary};
  text-align: center;

  font-size: 70px;
  line-height: 56px;
  width: 60px;
  padding: ${defaultMargins.s};

  @media screen and (max-width: 769px) {
    font-size: 48px;
    line-height: 52px;
    width: 48px;
    padding: ${defaultMargins.xs};
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
