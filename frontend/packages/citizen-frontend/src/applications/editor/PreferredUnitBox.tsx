import React from 'react'
import { PublicUnit } from '@evaka/lib-common/src/api-types/units'
import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from '@evaka/lib-components/src/layout/flex-helpers'
import { H4 } from '@evaka/lib-components/src/typography'
import IconButton from '@evaka/lib-components/src/atoms/buttons/IconButton'
import { faArrowDown, faArrowUp, faTimes } from '@evaka/lib-icons'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import InlineButton from '@evaka/lib-components/src/atoms/buttons/InlineButton'

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
          <FixedSpaceRow spacing={'xs'}>
            <div>kieli-chippi</div>
            <div>järjestämismuoto-chippi</div>
          </FixedSpaceRow>
          <FixedSpaceRow>
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
          </FixedSpaceRow>
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
  font-size: 70px;
  line-height: 56px;
  color: ${colors.primary};
  padding: ${defaultMargins.s};
  width: 60px;
  text-align: center;
`

const MainColCenter = styled.div`
  flex-grow: 1;
  padding: ${defaultMargins.s} 0;
`

const MainColRight = styled.div`
  flex-grow: 0;
  padding: ${defaultMargins.s};
`
