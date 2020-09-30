// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Vue from 'vue'
import { Prop } from 'vue-property-decorator'
import Component from 'vue-class-component'
import { set, get } from 'lodash'

import {
  required as requiredValidator,
  Validator,
  ValidationError
} from '../validation/input-validation'
import CForm from './c-form'
import { withAsterisk } from '@/utils/helpers'

type InputDisplayState = '' | 'danger' | 'success' | 'warning'

@Component
export default class BaseInput extends Vue {
  @Prop()
  public name: string | string[]

  @Prop({
    required: true,
    default: () => ({})
  })
  public value: any

  @Prop()
  public label: string

  @Prop()
  public placeholder: string

  @Prop()
  public validationName: string

  @Prop()
  public icon: string[]

  @Prop({
    default: null
  })
  public leftIcon: string[]

  @Prop({
    default: false
  })
  public required: boolean

  @Prop({
    default: false
  })
  public disabled: boolean

  @Prop({
    default: () => []
  })
  public validators: Validator[]

  public id: string = Math.random()
    .toString(32)
    .slice(2)
  public touched: boolean = false

  get labelWithAsterisk(): string {
    const { required, label } = this
    return required ? withAsterisk(label) : label
  }

  get _inputValue(): any {
    return this.name ? get(this.value, this.inputName) : this.value || ''
  }

  public handleInput(e: any) {
    const v = e.target.value as string
    this.$emit('input', this.getUpdateValue(v))
  }

  get inputName(): string {
    const name = this.name as any
    return name instanceof Array ? name.filter((n) => n).join('.') : name
  }

  get inputValidators(): Validator[] {
    return [...(this.required ? [requiredValidator] : []), ...this.validators]
  }

  get validationErrors(): ValidationError[] {
    return this.inputValidators
      .map((v) => v(this.inputName, this.getValidationName)(this._inputValue))
      .filter((v) => v.kind === 'error') as ValidationError[]
  }

  get validationResult(): ValidationError | null {
    return (this.touched && this.validationErrors[0]) || null
  }

  get state(): InputDisplayState {
    return this.validationResult ? 'danger' : ''
  }

  get message(): string {
    return this.validationResult ? this.validationResult.message : ''
  }

  get getValidationName(): string {
    return this.validationName
      ? this.validationName
      : this.label
      ? this.label
      : ''
  }

  get inputIcon() {
    return this.state === 'danger' ? ['fal', 'exclamation-triangle'] : this.icon
  }

  public getUpdateValue(value) {
    return set(
      {
        ...this.value
      },
      this.inputName,
      value
    )
  }

  public handleBlur(e: any) {
    this.touched = true
    this.handleInput(e)
    this.$emit('blur', e)
  }

  public handleFocus(e: any) {
    this.$emit('focus', e)
  }

  public mounted() {
    const form = this.findParentForm() as CForm
    if (form) {
      form.registerField(this)
    }
  }

  public beforeDestroy() {
    const form = this.findParentForm() as CForm
    if (form) {
      form.removeField(this)
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
