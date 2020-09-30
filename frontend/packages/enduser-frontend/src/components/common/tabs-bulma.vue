<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div :class="{ 'vue-bulma-tabs': true, [`is-layout-${layout}`]: true }">
    <div
      :class="{
        tabs: true,
        [`is-${size}`]: size,
        [`is-${alignment}`]: alignment,
        [`is-${type}`]: type,
        'is-fullwidth': isFullwidth
      }"
    >
      <slot name="left-tab-list"></slot>
      <tab-list>
        <li
          v-for="(tab, index) in tabPanes"
          :key="tab.name"
          role="tab"
          :class="[
            'is-tab',
            { 'is-active': isActived(index), 'is-disabled': tab.disabled }
          ]"
          :aria-selected="isActived(index) ? 'true' : 'false'"
          :aria-expanded="isActived(index) ? 'true' : 'false'"
          :aria-disabled="tab.disabled ? 'true' : 'false'"
          :selected="isActived(index)"
          :disabled="tab.disabled"
          :aria-label="tab.title"
          @click="select(index)"
          @keyup.enter="select(index)"
          tabindex="0"
        >
          <span class="tab-selector">
            <span class="fav-selected" v-if="tab.favSelected">{{
              tab.favSelected
            }}</span>
            <span class="icon is-small tab-icons" v-if="tab.icon">
              <font-awesome-icon :icon="['far', tab.icon]"></font-awesome-icon>
            </span>
            <span class="tab-title">{{ tab.title }}</span>
          </span>
        </li>
      </tab-list>
      <slot name="right-tab-list"></slot>
    </div>
    <div class="tab-content is-flex">
      <slot></slot>
    </div>
  </div>
</template>

<script>
  import TabList from '@/components/common/tablist-bulma.vue'

  export default {
    components: {
      TabList
    },

    props: {
      isFullwidth: Boolean,
      layout: {
        type: String,
        default: 'top',
        validator(val) {
          return ['top', 'bottom', 'left', 'right'].indexOf(val) > -1
        }
      },
      type: {
        type: String,
        default: ''
      },
      size: {
        type: String,
        default: ''
      },
      alignment: {
        type: String,
        default: ''
      },
      selectedIndex: {
        type: Number,
        default: -1
      },
      animation: {
        type: String,
        default: 'fade'
      },
      onlyFade: {
        type: Boolean,
        default: true
      }
    },

    data() {
      return {
        realSelectedIndex: this.selectedIndex,
        tabPanes: []
      }
    },
    methods: {
      update() {
        for (const tabPane of this.tabPanes) {
          if (!tabPane.disabled && tabPane.realSelected) {
            this.select(tabPane.index)
            break
          }
        }
      },
      isActived(index) {
        return index === this.realSelectedIndex
      },
      select(index) {
        this.$emit('tab-selected', index)
        this.realSelectedIndex = index
      }
    },
    watch: {
      selectedIndex(newIndex) {
        if (this.selectedIndex !== this.realSelectedIndex) {
          this.select(newIndex)
        }
      }
    }
  }
</script>

<style lang="scss">
  .vue-bulma-tabs {
    position: relative;
    display: flex;
    flex: 1;

    &.is-layout-top {
      flex-direction: column;
    }

    .tabs {
      overflow: visible;

      &:after {
        content: '';
        display: table;
        overflow: hidden;
        clear: both;
      }

      &.is-toggle li.is-active a {
        z-index: auto;
      }
    }

    .is-tab {
      padding: 0.5rem;
      cursor: pointer;

      &:hover {
        opacity: 0.75;
      }

      .tab-selector {
        position: relative;
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        padding-bottom: 0.25rem;

        .icon {
          margin-bottom: 0.5rem;
        }

        .tab-title {
          text-transform: uppercase;
          font-weight: 600;
          font-size: 0.875rem;
        }
      }

      &.is-active {
        .tab-selector {
          &:after {
            content: '';
            position: absolute;
            left: 0;
            bottom: -6px;
            width: 100%;
            border-bottom: 6px solid #3273dc;
          }
        }
      }
    }

    .tab-content {
      flex-direction: column;
      overflow: hidden;
      position: relative;
      margin: 10px 30px;
      flex: 1 1;
    }

    .tab-icons {
      &.icon {
        margin: 0 auto;
      }
    }

    .fav-selected {
      position: absolute;
      right: 0.25rem;
      top: -0.75rem;
      background-color: #00d1b2;
      color: white;
      font-weight: bold;
      font-size: 0.75rem;
      padding: 0.25rem 0.425rem;
      line-height: 1;
      border-radius: 50%;
    }
  }

  .is-disabled {
    pointer-events: none;
    opacity: 0.5;
  }
</style>
