import { EvakaColors } from '@evaka/frontend-customization-interfaces/'

const primaryColors = {
  dark: '#013c8c',
  medium: '#0050bb',
  primary: '#3273c9',
  light: '#99b9e4'
}

const colors: EvakaColors = {
  cityBrandColors: {
    primary: '#0050bb',
    secondary: '#249fff'
  },
  primaryColors,
  primary: primaryColors.primary,
  primaryHover: primaryColors.medium,
  primaryActive: primaryColors.dark,
  greyscale: {
    darkest: '#0f0f0f',
    dark: '#6e6e6e',
    medium: '#b1b1b1',
    lighter: '#d8d8d8',
    lightest: '#f5f5f5',
    white: '#ffffff'
  },
  accents: {
    orange: '#ff7300',
    orangeDark: '#b85300',
    green: '#c6db00',
    greenDark: '#6e7a00',
    water: '#9fc1d3',
    yellow: '#ffce00',
    red: '#db0c41',
    petrol: '#1f6390',
    emerald: '#038572',
    violet: '#9d55c3'
  }
}

export default colors
