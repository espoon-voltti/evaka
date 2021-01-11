import { accentColors, greyscale } from '@evaka/lib-components/src/colors'
import { faGavel, faCheck, faTimes } from '@evaka/lib-icons'
import styled from 'styled-components'

export const Status = styled.span`
  text-transform: uppercase;
`

export const statusIcon = {
  PENDING: {
    icon: faGavel,
    color: accentColors.orange
  },
  ACCEPTED: {
    icon: faCheck,
    color: accentColors.green
  },
  REJECTED: {
    icon: faTimes,
    color: greyscale.lighter
  }
}
