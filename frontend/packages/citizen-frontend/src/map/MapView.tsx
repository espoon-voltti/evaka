import React from 'react'
import styled from 'styled-components'
import Container, {
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import { defaultMargins } from '@evaka/lib-components/src/white-space'

// todo: improve layout

const FlexContainer = styled.div`
  display: flex;
  flex-wrap: wrap;
  margin-top: ${defaultMargins.L};
`

const SearchContainer = styled(ContentArea)`
  box-sizing: border-box;
  min-width: 400px;
  min-height: 600px;
  flex-grow: 1;
`

const MapContainer = styled.div`
  min-width: 400px;
  height: 100%;
  min-height: 600px;
  background-color: #8bb893;
  flex-grow: 99;
`

export default React.memo(function MapView() {
  return (
    <Container>
      <FlexContainer>
        <SearchContainer opaque>filtterit yms</SearchContainer>
        <MapContainer>kartta</MapContainer>
      </FlexContainer>
    </Container>
  )
})
