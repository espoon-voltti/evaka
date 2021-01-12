import Customizations, { Colors } from '@evaka/frontend-interface/src/index'
import citizenLocalization from './localization/citizen'

const colors: Colors = {
  primaryColors: {
    dark: '#013c8c',
    medium: '#0050bb',
    primary: '#3273c9',
    light: '#99b9e4'
  }
}

const customizations: Customizations = {
  colors,
  localization: {
    citizen: citizenLocalization
  }
}

export default customizations
