import Customizations, { Colors } from '@evaka/frontend-interface/src/index'
import citizenLocalization from './localization/citizen'

const colors: Colors = {
  primaryColors: {
    dark: '#c48813',
    medium: '#ffd19d',
    primary: '#ffba48',
    light: '#ffeed8'
  }
}

const customizations: Customizations = {
  colors,
  localization: {
    citizen: citizenLocalization
  }
}

export default customizations
