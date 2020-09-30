<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="validation-wrapper" :class="wrapperClasses">
    <slot></slot>
    <div class="validation-errors">
      <span v-show="hasErrors" class="validation-error help has-error">
        <span
          v-for="(error, index) in errors"
          :key="index"
          class="validation-error-text"
        >
          {{ error }}
        </span>
      </span>
    </div>
  </div>
</template>

<script>
  import _ from 'lodash'
  import form, { bind } from '@/mixins/form'
  import uuid from '@/mixins/uuid'

  export default {
    mixins: [form, uuid],
    props: {
      name: {
        type: String
      },
      value: {
        type: null
      },
      validators: {
        type: Array
      }
    },
    inject: ['section'],
    data() {
      return {
        [this.name]: this.value
      }
    },
    created() {
      this.updateValidation()
    },
    beforeDestroy() {
      this.$store.dispatch('removeValidation', this.uuid)
    },
    watch: {
      value(value) {
        this[this.name] = value
        if (!this.isDirty) {
          this.$v.$touch()
        }
        this.updateValidation()
      },
      validators() {
        this.updateValidation()
      }
    },
    computed: {
      validateAll: bind('editing', 'application.validateAll'),
      validator() {
        return this.$v[this.name]
      },
      isDirty() {
        return this.validateAll || this.$v.$dirty
      },
      isInvalid() {
        return this.$v.$invalid
      },
      hasErrors() {
        return this.isDirty && this.isInvalid
      },
      activeValidations() {
        return _.omitBy(this.validator, (val, key) => key.startsWith('$'))
      },
      errorMessages() {
        const findFirstKey = (o) => _.findKey(o, _.stubTrue)
        const errorMsgByValidationName = _.map(this.validators, (v) => ({
          [findFirstKey(v.validation)]: this.formatErroMessage(v.errorMsg)
        }))
        return Object.assign({}, ...errorMsgByValidationName)
      },
      errors() {
        return _(this.activeValidations)
          .omitBy(_.identity)
          .keys()
          .map((v) => this.errorMessages[v])
          .value()
      },
      wrapperClasses() {
        return {
          'has-errors': this.hasErrors,
          'is-dirty': this.isDirty
        }
      }
    },
    methods: {
      formatErroMessage(msg) {
        return msg
          .replace('#NAME#', this.name)
          .replace('#VALUE#', this[this.name])
      },
      updateValidation() {
        this.$store.dispatch('updateValidation', {
          [this.uuid]: {
            uuid: this.uuid,
            name: this.name,
            section: this.section,
            validator: {
              dirty: this.isDirty,
              invalid: this.isInvalid,
              msgs: this.errors
            }
          }
        })
      }
    },
    validations() {
      return {
        [this.name]: Object.assign(
          {},
          ...this.validators.map((v) => v.validation)
        )
      }
    }
  }
</script>

<style lang="scss" scoped>
  .validation-wrapper {
    margin: 0;
    padding: 0;
  }

  .validation-errors {
    height: 0.875rem;
    margin: 0.25rem 0;

    .validation-error {
      margin: 0;
      font-size: 0.75rem;
    }

    .validation-error-text {
      margin-right: 0.75rem;
    }
  }
</style>

<style lang="scss">
  // TODO: fix input layout
  .has-errors > div > p > .input {
    border-color: $color-error-border;
    color: black;
  }

  .has-errors > .multiselect {
    .multiselect__tags {
      border-color: $color-error-border;
    }
  }

  .has-errors > div > label {
    &::before {
      border: 2px solid red;
    }
  }
</style>
