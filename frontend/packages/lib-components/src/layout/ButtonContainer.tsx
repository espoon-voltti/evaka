import styled from 'styled-components'
import { tabletMin } from '../breakpoints'
import { defaultMargins } from '../white-space'

type Props = {
  justify?: 'flex-start' | 'flex-end' | 'center'
}

export default styled.div<Props>`
  display: flex;
  flex-wrap: nowrap;
  align-items: stretch;

  flex-direction: column;
  justify-content: center;

  > * {
    margin: 0;
  }

  @media (min-width: ${tabletMin}) {
    flex-direction: row-reverse;
    justify-content: ${({ justify: align }) => align ?? 'flex-end'};

    > *:not(:first-child) {
      margin-right: ${defaultMargins.s};
    }
  }

  @media (max-width: calc(${tabletMin} + -1px)) {
    > *:not(:last-child) {
      margin-bottom: ${defaultMargins.s};
    }
  }
`
