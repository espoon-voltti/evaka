<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <validation :name="validationName" :value="value" :validators="getValidators">
    <label v-if="label" class="label">
      {{ label }}: <span v-show="required">*</span>
      <c-instructions
        v-if="hasInstructions"
        :instruction="instructions"
      ></c-instructions>
    </label>
    <datepicker
      :config="getConfig"
      :value="value"
      :placeholder="placeholder"
      @input="onDatePicked"
      id="input-date-picker"
    >
    </datepicker>
  </validation>
</template>

<script>
  import _ from 'lodash'
  import { fi } from '@/localization/fi'
  import Datepicker from 'vue-bulma-datepicker'
  import Validation from '@/components/validation/validation.vue'
  import { required as requiredValidator } from '@/components/validation/validators.js'

  const DEFAULT_DATE_CONFIG = {
    minDate: 'today',
    dateFormat: 'd.m.Y',
    weekNumbers: true,
    static: true,
    locale: fi
  }

  export default {
    components: {
      Datepicker,
      Validation
    },
    props: {
      value: String,
      label: String,
      validationLabel: String,
      instructions: {
        type: String
      },
      required: Boolean,
      validators: {
        type: Array,
        default: () => []
      },
      placeholder: {
        type: String,
        default: 'Valitse päivämäärä'
      },
      dateConfig: {
        type: Object,
        default: () => ({})
      }
    },
    computed: {
      validationName() {
        return this.label || this.validationLabel
      },
      hasInstructions() {
        return !_.isEmpty(this.instructions)
      },
      getValidators() {
        return this.required
          ? [requiredValidator, ...this.validators]
          : this.validators
      },
      getConfig() {
        return Object.assign({}, DEFAULT_DATE_CONFIG, this.dateConfig)
      }
    },
    methods: {
      onDatePicked(date) {
        this.$emit('input', date)
      }
    }
  }
</script>

<style lang="scss">
  .has-errors {
    .flatpickr-input {
      border-color: $color-error-border;
    }
  }

  .date-selection {
    .flatpickr-calendar:before,
    .flatpickr-calendar:after {
      content: none;
      display: none;
    }
  }

  .dayContainer {
    .flatpickr-day.disabled {
      color: #f3b4b4;
    }
  }
</style>
