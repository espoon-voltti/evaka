import React from 'react'
import styled, { keyframes } from 'styled-components'

const rotate = keyframes`
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
`

const Spinner = styled.div`
  font-weight: 400;
  border-collapse: collapse;
  border-spacing: 0;
  color: #0f0f0f;
  text-align: left !important;
  line-height: 1.3em;
  box-sizing: inherit;
  border-radius: 50%;
  min-width: 65px;
  min-height: 65px;
  margin: 2em;
  font-size: 10px;
  text-indent: -9999em;
  border: 8px solid rgba(51, 115, 201, 0.2);
  border-left-color: #3373c9;
  transform: translateZ(0);
  animation: ${rotate} 1.1s linear infinite;
`

const Wrapper = styled.div`
  font-family: Open Sans, Arial, sans-serif;
  font-size: 1em;
  font-weight: 400;
  border-collapse: collapse;
  border-spacing: 0;
  color: #0f0f0f;
  text-align: left !important;
  line-height: 1.3em;
  box-sizing: inherit;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
`

function Loader() {
  return (
    <Wrapper>
      <Spinner />
    </Wrapper>
  )
}

export default Loader
