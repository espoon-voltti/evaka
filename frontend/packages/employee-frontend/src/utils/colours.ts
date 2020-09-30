// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

type EspooColour =
  | '#0f0f0f'
  | '#6e6e6e'
  | '#9e9e9e'
  | '#c4c4c4'
  | '#d8d8d8'
  | '#ffffff'
  | '#3273c9'
  | '#0050bb'
  | '#249fff'
  | '#9fc1d3'
  | '#1f6390'
  | '#c6db00'
  | '#ffce00'
  | '#ff7300'
  | '#db0c41'

const greyDarker: EspooColour = '#0f0f0f'
const greyDark: EspooColour = '#6e6e6e'
const grey: EspooColour = '#9e9e9e'
const greyLight: EspooColour = '#c4c4c4'
const greyLighter: EspooColour = '#d8d8d8'
const white: EspooColour = '#ffffff'
const espooBlue: EspooColour = '#0050bb'
const espooTurqoise: EspooColour = '#249fff'
const water: EspooColour = '#9fc1d3'
const petrol: EspooColour = '#1f6390'
const green: EspooColour = '#c6db00'
const yellow: EspooColour = '#ffce00'
const orange: EspooColour = '#ff7300'
const red: EspooColour = '#db0c41'

const bluePrimary = '#3273c9'
const greyVeryLight = '#f9f9f9'
const darkGreen = '#038572'
const purple = '#9d55c3'

export const EspooColours = {
  greyDarker,
  greyDark,
  grey,
  greyLight,
  greyLighter,
  white,
  bluePrimary,
  espooBlue,
  espooTurqoise,
  water,
  petrol,
  green,
  yellow,
  orange,
  red
}

export const customColours = {
  greyVeryLight,
  darkGreen,
  purple,
  bluePrimary
}

type AbsenceColour = '#3273c9' | '#013c8c' | '#9d55c3' | '#99b9e4'
const blueDark: AbsenceColour = '#013c8c'
const lila: AbsenceColour = '#9d55c3'
const blueLight: AbsenceColour = '#99b9e4'

export const AbsenceColours = {
  Unknown: green,
  Parentleave: bluePrimary,
  Uncharged: red,
  Other: blueDark,
  Sick: lila,
  Planned: blueLight,
  Visitor: yellow,
  Relocated: orange,
  Empty: greyLighter,
  Presence: greyLighter
}
