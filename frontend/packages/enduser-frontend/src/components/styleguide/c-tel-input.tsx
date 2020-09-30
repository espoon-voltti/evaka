// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Component } from 'vue-property-decorator'
import {
  tel,
  required as requiredValidator
} from '../validation/input-validation'
import Input from './c-input'

@Component
class TelInput extends Input {
  get inputValidators() {
    return [
      ...(this.required ? [requiredValidator] : []),
      tel,
      ...this.validators
    ]
  }

  public handleInput(e: any) {
    const v = e.target.value as string
    if (!tel(this.inputName, this.getValidationName)(v)) {
      return
    }
    this.$emit('input', this.getUpdateValue(v))
  }
}

export default TelInput
