<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <validation :name="label" :value="value" :validators="getValidators">
    <form-field
      name="identityNumber"
      v-model="inputValue"
      @blur="handleBlur"
      :label="label"
      :placeholder="placeholder"
      :icon="icon"
      :disabled="disabled"
      :required="required"
    >
    </form-field>
  </validation>
</template>

<script>
  import FormField from '@/components/form-controls/form-field.vue'
  import Validation from '@/components/validation/validation.vue'
  import {
    required as requiredValidator,
    identityNumber,
    isValidIdentityNumber
  } from '@/components/validation/validators.js'

  export default {
    components: {
      FormField,
      Validation
    },
    props: {
      value: String,
      placeholder: {
        type: String,
        default: '010111A1234'
      },
      required: {
        type: Boolean,
        default: false
      },
      icon: {
        type: String,
        default: 'id-card'
      },
      validators: {
        type: Array,
        default: () => [identityNumber]
      },
      label: {
        type: String,
        default: 'Henkilötunnus'
      },
      disabled: Boolean
    },
    computed: {
      getValidators() {
        return this.required
          ? [requiredValidator, ...this.validators]
          : this.validators
      },

      inputValue: {
        get() {
          return this.value
        },
        set(value) {
          if (isValidIdentityNumber(value)) {
            this.$emit('input', value)
          }
        }
      }
    },
    methods: {
      handleBlur(e) {
        this.$emit('input', e.target.value)
      }
    }
  }
</script>
