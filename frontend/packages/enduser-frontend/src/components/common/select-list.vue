<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <multi-select
    :placeholder="placeholder"
    :value="selectedValue"
    :options="filteredOptions"
    selectedLabel=""
    deselectLabel="X"
    selectLabel=""
    track-by="name"
    :multiple="false"
    :searchable="true"
    :internal-search="false"
    :clear-on-select="true"
    :reset-after="resetAfter"
    :loading="false"
    id="ajax"
    :custom-label="customLabel"
    :max-height="300"
    @search-change="searchOptions"
    @input="valueChanged"
    :disabled="isDisabled"
  >
    <span slot="noResult">{{ $t('general.no-result') }}</span>
  </multi-select>
</template>

<script>
  import MultiSelect from 'vue-multiselect'
  import _ from 'lodash'

  export default {
    props: {
      value: String,
      isRequired: Boolean,
      options: Array,
      placeholder: String,
      resetAfter: {
        type: Boolean,
        default: false
      },
      isDisabled: {
        type: Boolean,
        default: false
      }
    },
    components: {
      MultiSelect
    },
    data() {
      return {
        query: ''
      }
    },
    computed: {
      filteredOptions() {
        if (this.query) {
          return this.options.filter(
            (el) =>
              el.value.toLowerCase().indexOf(this.query.toLowerCase()) > -1
          )
        } else {
          return this.options
        }
      },
      selectedValue() {
        if (this.value) {
          return _.find(
            this.options,
            (option) => option.name.toLowerCase() === this.value.toLowerCase()
          )
        }
      },
      required() {
        return this.isRequired === 'true' ? 'required' : ''
      }
    },
    methods: {
      customLabel(option) {
        return `${option.value}`
      },
      searchOptions(query) {
        this.query = query
      },
      valueChanged(value) {
        if (value) {
          this.$emit('input', value.name)
        } else {
          this.$emit('input', value)
        }
      }
    }
  }
</script>

<style lang="scss">
  .multiselect {
    cursor: pointer;
  }

  .multiselect__content-wrapper {
    z-index: 9;
  }

  .multiselect--disabled {
    cursor: not-allowed;
    pointer-events: auto;

    > * {
      pointer-events: none;
    }
  }
</style>
