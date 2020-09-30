<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="radio-group field" :class="{ 'has-addons': screenSize }">
    <div v-for="btn in values" class="control">
      <label
        class="radio button"
        :class="{ selected: selected(btn) }"
        tabindex="0"
      >
        <input
          :name="_uid"
          type="radio"
          @change.prevent="onChange(btn.value)"
        />
        {{ btn.label }}
      </label>
    </div>
  </div>
</template>

<script>
  import _ from 'lodash'

  export default {
    data() {
      return {
        windowWidth: 0
      }
    },
    props: {
      value: [String, Object],
      values: {
        validator: (values) => {
          return (
            !_.isEmpty(values) &&
            _.every(
              values,
              (val) => !_.isUndefined(val.label) && !_.isUndefined(val.value)
            )
          )
        }
      },
      selector: {
        type: Function,
        default: _.isEqual
      }
    },
    computed: {
      screenSize() {
        return this.windowWidth > 1200
      }
    },
    methods: {
      selected(btn) {
        return this.selector(this.value, btn.value)
      },
      onChange(value) {
        this.$emit('input', value)
      },
      resetWindowWidth() {
        this.windowWidth = document.documentElement.clientWidth
      }
    },
    mounted() {
      this.$nextTick(function() {
        window.addEventListener('resize', this.resetWindowWidth)
        // Init
        this.resetWindowWidth()
      })
    }
  }
</script>

<style lang="scss" scoped>
  .radio-group {
    &:not(.has-addons) {
      label {
        width: 100%;

        @include onMobile() {
          width: 14rem;
        }
      }
    }

    label.button:focus {
      box-shadow: 0 0 0 2px #00d1b2;
      border-color: white;
    }
  }
</style>
