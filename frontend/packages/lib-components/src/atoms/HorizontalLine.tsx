import styled from 'styled-components'
import { defaultMargins } from '../white-space'
import colors from '../colors'

const HorizontalLine = styled.hr`
  width: 100%;
  margin-block-start: ${defaultMargins.XL};
  margin-block-end: ${defaultMargins.XL};
  border: none;
  border-bottom: 1px solid ${colors.greyscale.lighter};
`

export default HorizontalLine
