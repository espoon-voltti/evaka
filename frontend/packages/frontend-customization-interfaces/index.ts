import citizenLocalization from "./src/CitizenLocalization";
import colors from "./src/EvakaColors";

export type CitizenLocalization = citizenLocalization
export type EvakaColors = colors

export default interface Customizations {
  colors: EvakaColors,
  localization: {
    citizen: {
      fi: CitizenLocalization
      sv: CitizenLocalization
      en: CitizenLocalization
    }
  }
}
