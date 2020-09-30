<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div
    role="tabpanel"
    v-show="realSelected"
    :class="classObject"
    :aria-hidden="hidden"
  >
    <slot></slot>
  </div>
</template>

<script>
  export default {
    props: {
      icon: String,
      ariaLabel: String,
      favSelected: Number,
      selected: Boolean,
      disabled: Boolean,
      title: {
        type: String,
        required: true
      },
      mode: {
        type: String,
        default: 'out-in'
      }
    },

    data() {
      return {
        realSelected: this.selected
      }
    },

    created() {
      this.$parent.tabPanes.push(this)
    },

    beforeDestroy() {
      this.$parent.tabPanes.splice(this.index, 1)
    },

    computed: {
      classObject() {
        const { realSelected } = this
        return {
          'tab-pane': true,
          'is-active': realSelected,
          'is-deactive': !realSelected
        }
      },
      layout() {
        return this.$parent.layout
      },
      direction() {
        if (this.layout === 'top' || this.layout === 'bottom') {
          return 'horizontal'
        } else if (this.layout === 'left' || this.layout === 'right') {
          return 'vertical'
        }
        return ''
      },
      animation() {
        return this.$parent.animation
      },
      onlyFade() {
        return this.$parent.onlyFade
      },
      lt() {
        if (this.direction === 'vertical') {
          return 'dtu'
        } else if (this.direction === 'horizontal') {
          return 'rtl'
        }
        return ''
      },
      hidden() {
        return this.realSelected ? 'false' : 'true'
      },
      index() {
        return this.$parent.tabPanes.indexOf(this)
      }
    },

    watch: {
      '$parent.realSelectedIndex'(index, prevIndex) {
        this.realSelected = this.index === index
      }
    }
  }
</script>

<style lang="scss">
  .tab-pane {
    width: 100%;
    flex: 1 1;
    transition: all ease 0.5s;

    &[class*='Out'] {
      overflow: hidden;
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      transform: translateX(0) translateY(0);
    }
  }
</style>
