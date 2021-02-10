import React from 'react'
import styled from 'styled-components'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import colors from '@evaka/lib-components/src/colors'
import { FooterContent } from '~Footer'

export default React.memo(function SearchSection() {
  return (
    <Wrapper className="map-box">
      <Map />
      <FooterWrapper>
        <FooterContent />
      </FooterWrapper>
    </Wrapper>
  )
})

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

const Map = styled.div`
  flex-grow: 1;
  width: 100%;
  min-height: 500px;
  background-color: #c1e2c9;
`

const FooterWrapper = styled.div`
  position: absolute;
  left: 0;
  bottom: 0;
  width: fit-content;
  background-color: ${colors.greyscale.white};

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
