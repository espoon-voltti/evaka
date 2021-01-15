import styled from 'styled-components'
import { defaultMargins } from '../white-space'
import colors from '../colors'

const HorizontalLine = styled.hr`
  margin-block-start: ${defaultMargins.XL};
  margin-block-end: ${defaultMargins.XL};
  border: 1px solid ${colors.greyscale.lighter};
`

export default HorizontalLine
