<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div
    class="card collapse-item"
    :class="{ 'is-active': show, 'has-errors': hasErrors }"
  >
    <header
      class="card-header touchable"
      role="tab"
      :aria-expanded="isSelected"
      @click="toggle"
      ref="header"
      tabindex="0"
      @keyup.enter="toggle"
    >
      <h4 class="card-header-title">{{ title }}</h4>
      <warnings-tooltip
        class="card-header-icon warning-tooltip"
        v-if="hasErrors"
        :warnings="errorMessages"
        :isError="isError"
      >
      </warnings-tooltip>
      <!-- Arrow -->
      <span class="card-header-icon card-header-arrow">
        <font-awesome-icon :icon="['far', 'angle-right']"></font-awesome-icon>
      </span>
    </header>
    <div class="card-content" v-show="show">
      <div class="card-content-box">
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<script>
  import _ from 'lodash'
  import uuid from '@/mixins/uuid'
  import form, { bind } from '@/mixins/form'
  import WarningsTooltip from '@/components/tooltip/warnings-tooltip.vue'
  import { scrollToElement } from '@/utils/scroll-to-element'

  export default {
    mixins: [uuid, form],
    components: {
      WarningsTooltip
    },
    props: {
      selected: Boolean,
      title: {
        type: String,
        required: true
      }
    },
    provide() {
      return {
        section: {
          uuid: this.uuid,
          name: this.title
        }
      }
    },
    created() {
      if (this.selected) {
        this.$store.dispatch('toggleSection', this.uuid)
      }
    },
    computed: {
      validateAll: bind('editing', 'application.validateAll'),
      activeSection: bind('editing', 'application.activeSection'),
      show() {
        return this.uuid === this.activeSection
      },
      validations() {
        return this.$store.getters.validationsBySection(this.uuid)
      },
      validationErrors() {
        return _.filter(
          this.validations,
          (v) => v.validator.dirty && v.validator.invalid
        )
      },
      errorCount() {
        return this.validationErrors.length
      },
      hasErrors() {
        return this.errorCount > 0
      },
      isError() {
        return this.validateAll
      },
      errorMessages() {
        return _(this.validationErrors)
          .map((v) => v.validator.msgs)
          .flatten()
          .value()
      },
      isSelected() {
        return this.uuid === this.activeSection ? 'true' : 'false'
      }
    },
    methods: {
      toggle() {
        this.$store.dispatch('toggleSection', this.uuid).then(() => {
          if (this.show) {
            window.requestAnimationFrame(() => {
              const element = this.$refs.header
              if (element instanceof Element && document.contains(element)) {
                scrollToElement(element)
              }
            })
          }
        })
      }
    }
  }
</script>

<style lang="scss" scoped>
  .collapse-item {
    border-radius: 8px;

    &.has-errors {
      border-color: $red;
    }

    .card-header {
      cursor: pointer;
    }

    .card-header-icon {
      transition: transform 0.377s ease;
    }

    .card-content {
      padding-top: 0;
      padding-bottom: 0;
      overflow: visible;
    }

    .card-content-box {
      padding-top: 20px;
      padding-bottom: 20px;
    }

    &.is-active {
      .card-header-arrow {
        transform: rotate(90deg);
      }
    }
  }
</style>
