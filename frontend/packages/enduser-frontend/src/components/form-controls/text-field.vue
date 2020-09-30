<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <validation :name="label" :value="value" :validators="getValidators">
    <form-field
      :name="name"
      :value="value"
      :label="label"
      :placeholder="placeholder"
      :icon="icon"
      inputType="text"
      @input="valueChanged"
      :disabled="disabled"
      :required="required"
    >
    </form-field>
  </validation>
</template>

<script>
  import Validation from '@/components/validation/validation.vue'
  import { required as requiredValidator } from '@/components/validation/validators.js'
  import FormField from '@/components/form-controls/form-field.vue'

  export default {
    components: {
      Validation,
      FormField
    },
    data() {
      return {
        active: false
      }
    },
    props: {
      value: String,
      name: String,
      label: String,
      placeholder: String,
      required: {
        type: Boolean,
        default: false
      },
      icon: String,
      inputType: {
        default: 'text',
        type: String
      },
      validators: {
        type: Array,
        default: () => []
      },
      disabled: Boolean
    },
    computed: {
      getValidators() {
        return this.required
          ? [requiredValidator, ...this.validators]
          : this.validators
      }
    },
    methods: {
      valueChanged(value) {
        this.$emit('input', value)
      }
    }
  }
</script>

<style lang="scss">
  input.has-error {
    border-color: red;
    color: black;
  }

  .has-error {
    color: red;
  }

  ::-webkit-input-placeholder {
    /* WebKit, Blink, Edge */
    opacity: 0.75;
    font-size: 85%;
    font-style: italic;
  }
  :-moz-placeholder {
    /* Mozilla Firefox 4 to 18 */
    opacity: 0.75;
    font-size: 85%;
    font-style: italic;
  }
  ::-moz-placeholder {
    /* Mozilla Firefox 19+ */
    opacity: 0.75;
    font-size: 85%;
    font-style: italic;
  }
  :-ms-input-placeholder {
    /* Internet Explorer 10-11 */
    opacity: 0.75;
    font-size: 85%;
    font-style: italic;
  }

  .control.has-icons-left {
    .icon {
      z-index: 1;
    }
  }
</style>
