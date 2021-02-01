import styled from 'styled-components'
import { defaultMargins } from '../white-space'
import { tabletMin } from '../breakpoints'

type ActionRowProps = {
  breakpoint?: string
}

const ActionRow = styled.div<ActionRowProps>`
  display: flex;
  flex-direction: row;
  align-items: center;

  > * {
    margin-right: ${defaultMargins.s};
    &:last-child {
      margin-right: 0;
    }
  }

  .expander {
    flex-grow: 1;
  }

  @media (max-width: ${(p) => p.breakpoint ?? tabletMin}) {
    flex-direction: column-reverse;
    padding: 0 ${defaultMargins.s};

    > * {
      width: 100%;
      margin-right: 0;
      margin-bottom: 16px;
    }

    .expander {
      display: none;
    }
  }
`

export default ActionRow
