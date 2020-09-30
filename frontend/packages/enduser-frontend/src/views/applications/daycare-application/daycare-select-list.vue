<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <multi-select
    id="ajax"
    class="daycare-select-list"
    :placeholder="placeholder"
    :options="viewOptions"
    track-by="id"
    :multiple="false"
    :searchable="true"
    :internal-search="false"
    :clear-on-select="true"
    :disabled="disabled"
    :custom-label="customLabel"
    label="description"
    :max-height="300"
    deselectLabel="X"
    @input="onSelect"
    @search-change="onSearchChange"
    selectedLabel=""
    selectLabel=""
    :tabindex="disabled ? -1 : 0"
  >
    <template slot="option" slot-scope="props">
      <div class="group-wrapper select-list-item">
        <div class="group">
          <div class="group-content">
            <span class="strong description">{{ props.option.name }}</span>
            <span class="schedule" v-if="props.option.description">{{
              props.option.description
            }}</span>
            <span class="schedule">{{ props.option.streetAddress }}</span>
          </div>
          <span class="add-btn">
            <span>{{ $t('general.select') }}</span>
          </span>
        </div>
      </div>
    </template>

    <span slot="noResult">{{ $t('general.no-result') }}</span>

    <span slot="noOptions">{{ $t('general.no-result-short') }}</span>
  </multi-select>
</template>

<script lang="ts">
  import Vue, { PropType } from 'vue'
  import MultiSelect from 'vue-multiselect'
  import { Unit } from '@/types'
  import _ from 'lodash'

  interface ListObject extends Unit {
    $isDisabled: boolean
  }

  export default Vue.extend({
    data() {
      return {
        query: ''
      }
    },
    props: {
      options: Array as () => Unit[],
      placeholder: String,
      disabled: Boolean,
      selected: {
        type: Array as PropType<string[]>,
        default: (): string[] => []
      }
    },
    components: {
      MultiSelect
    },
    computed: {
      filteredOptions(): Unit[] {
        if (this.query) {
          return this.options.filter(
            (e) => e.name.toLowerCase().indexOf(this.query.toLowerCase()) > -1
          )
        }
        return this.options
      },

      viewOptions(): ListObject[] {
        const units: ListObject[] = this.options.map((o) => ({
          ...o,
          $isDisabled: !!this.selected.find((s) => s === o.id)
        }))

        const filtered = this.query
          ? units.filter(
              (el) =>
                el.name.toLowerCase().indexOf(this.query.toLowerCase()) > -1
            )
          : units
        return _.orderBy(filtered, ['name'])
      }
    },
    methods: {
      onSearchChange(q: string) {
        this.query = q
      },
      customLabel(option: Unit) {
        return `${option.name}`
      },

      onSelect(value: Unit) {
        this.$emit('select', value)
      }
    }
  })
</script>

<style lang="scss">
  .daycare-select-list {
    .title {
      word-wrap: break-word;
      white-space: normal;
    }

    .group {
      max-width: 100%;
      display: flex;
      flex-direction: row;
      align-items: center;

      &-content {
        flex: 1 1 auto;
        display: flex;
        flex-direction: column;
        white-space: normal;
        word-break: break-word;

        > span:not(:last-child) {
          margin-bottom: 8px;
        }
      }

      &-label {
        word-wrap: break-word;
        white-space: normal;
      }

      .add-btn {
        color: $blue-light;
        display: inline-block;
        padding: 8px;
      }

      @include onMobile() {
        flex-direction: column;
      }
    }

    .multiselect__content {
      display: flex !important;
      flex-direction: column;

      .multiselect__option {
        border-bottom: 1px solid $grey-lighter;
      }
    }

    .multiselect__tags {
      border-color: $grey-lighter;
    }

    .multiselect__option--highlight {
      background-color: $blue-lighter;

      &:hover {
        .add-btn {
          color: white;
        }
      }
    }

    .multiselect__option--disabled {
      background-color: $blue-light !important;
      color: white !important;
    }
  }
</style>
