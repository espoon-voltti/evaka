import Customizations from "@evaka/frontend-customization-interfaces";
import citizenFi from './src/localization/citizen/fi'
import citizenSv from './src/localization/citizen/sv'
import citizenEn from './src/localization/citizen/en'
import colors from './src/colors'

const customizations: Customizations = {
  colors,
  localization: {
    citizen: {
      fi: citizenFi,
      sv: citizenSv,
      en: citizenEn
    }
  }
}

export default customizations
