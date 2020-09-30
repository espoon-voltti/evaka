// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ssn,
  required as requiredValidator
} from '../validation/input-validation'
import Input from './c-input'

import { Component } from 'vue-property-decorator'

@Component
class SsnInput extends Input {
  get inputValidators() {
    return [
      ...(this.required ? [requiredValidator] : []),
      ssn,
      ...this.validators
    ]
  }

  public handleInput(e: any) {
    const v = e.target.value as string
    if (!ssn(this.inputName, this.getValidationName)(v)) {
      return
    }
    this.$emit('input', this.getUpdateValue(v))
  }
}

export default SsnInput
