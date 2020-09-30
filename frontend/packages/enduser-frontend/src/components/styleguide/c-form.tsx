// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Vue, { CreateElement } from 'vue'
import Component from 'vue-class-component'

import { set } from 'lodash'
import { ValidationError, Validator } from '../validation/input-validation'
import BaseInput from './base-input'
import { Prop, Watch } from 'vue-property-decorator'
import _ from 'lodash'

interface InputValidation {
  value: string | number | boolean
  validators: Validator[]
  errors: ValidationError[]
  touched: boolean
}

export type ValidationResult = Record<string, InputValidation>

@Component
class CForm extends Vue {
  @Prop({
    required: true
  })
  public name: string

  @Prop({
    required: true
  })
  public value: object

  @Prop({
    required: true
  })
  public data: object

  public fields: BaseInput[] = []
  public forms: CForm[] = []

  public validate(): ValidationResult {
    this.fields.map((f) => {
      f.touched = true
    })

    return {
      ...this.getInputValidators(),
      ...this.forms.reduce(
        (r, f) => ({
          ...r,
          ...f.validate()
        }),
        {}
      )
    }
  }

  @Watch('data', { immediate: true, deep: true })
  public updateValidation() {
    this.$emit(
      'input',
      set({ ...this.value }, this.name, this.getInputValidators())
    )
  }

  public getFieldValidationErrors(f: BaseInput): ValidationError[] {
    return f.inputValidators
      .map((v) => v(f.inputName, f.getValidationName)(f._inputValue))
      .filter((v) => v.kind === 'error') as ValidationError[]
  }

  public getValidationErrors(): ValidationError[] {
    const fields = this.fields.reduce(
      (r, f) => [...r, ...this.getFieldValidationErrors(f)],
      [] as ValidationError[]
    )

    const forms = this.forms.reduce((r, f) => {
      return [...r, ...f.getValidationErrors()]
    }, [] as any[])

    return [...fields, ...forms]
  }

  public getInputValidators(): ValidationResult {
    return this.fields.reduce((r, f) => {
      const v: InputValidation = {
        value: f._inputValue,
        validators: f.inputValidators,
        errors: this.getFieldValidationErrors(f),
        touched: f.touched
      }

      return {
        ...r,
        [f.inputName]: v
      }
    }, {})
  }

  get validationErrors(): any {
    return this.getValidationErrors()
  }

  public registerForm(form: CForm) {
    this.forms.push(form)
  }

  public registerField(field: BaseInput) {
    if (field.required) {
      this.fields.push(field)
    }
  }

  public removeField(field: BaseInput) {
    _.remove(this.fields, (f) => f.id === field.id)
  }

  public render(h: CreateElement) {
    return <div class="form-container">{this.$slots.default}</div>
  }

  public mounted() {
    const form = this.findParentForm() as CForm
    if (form) {
      form.registerForm(this)
    }
  }

  private findParentForm(): CForm | undefined {
    let parent = this.$parent as any
    while (parent.$parent) {
      if (parent instanceof CForm) {
        return parent
      }

      parent = parent.$parent
    }
    return undefined
  }
}

export default CForm
