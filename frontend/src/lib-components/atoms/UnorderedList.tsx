import styled from 'styled-components'
import { defaultMargins, isSpacingSize, SpacingSize } from '../white-space'

export default styled.ul<{ spacing?: SpacingSize | string }>`
  margin: 0;
  padding: 0 0 0 1.5em;

  li {
    &::marker {
      color: ${({ theme: { colors } }) => colors.brand.secondary};
    }
    margin-bottom: ${(p) =>
      p.spacing
        ? isSpacingSize(p.spacing)
          ? defaultMargins[p.spacing]
          : p.spacing
        : defaultMargins.s};

    &:last-child {
      margin-bottom: 0;
    }
  }
`
