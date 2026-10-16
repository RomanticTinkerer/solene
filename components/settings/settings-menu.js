import React, { Component } from 'react'
import { StyleSheet } from 'react-native'

import AppPage from '../common/app-page'
import MenuItem from './menu-item'
import AppText from '../common/app-text'

import {
  developerOptionsUnlockedObservable,
  saveDeveloperOptionsUnlocked,
} from '../../local-storage'
import { showToast } from '../helpers/general'
import settingsLabels from '../../i18n/en/settings'
import { Typography } from '../../styles'

const UNLOCK_TAPS_REQUIRED = 7
const TAP_RESET_MS = 2000

const { menuItems, developerOptions } = settingsLabels

export default class SettingsMenu extends Component {
  constructor(props) {
    super(props)

    this.state = {
      unlocked: developerOptionsUnlockedObservable.value,
      titleTapCount: 0,
    }
    this.resetTimer = null
  }

  componentDidMount() {
    this.unsubscribe = developerOptionsUnlockedObservable((unlocked) => {
      this.setState({ unlocked: !!unlocked })
    })
  }

  componentWillUnmount() {
    if (this.resetTimer) clearTimeout(this.resetTimer)
    if (typeof this.unsubscribe === 'function') this.unsubscribe()
  }

  onTitlePress = async () => {
    if (this.state.unlocked) return

    if (this.resetTimer) clearTimeout(this.resetTimer)

    const nextCount = this.state.titleTapCount + 1
    if (nextCount >= UNLOCK_TAPS_REQUIRED) {
      await saveDeveloperOptionsUnlocked(true)
      this.setState({ unlocked: true, titleTapCount: 0 })
      showToast(developerOptions.unlockedToast)
      return
    }

    this.setState({ titleTapCount: nextCount })
    this.resetTimer = setTimeout(() => {
      this.setState({ titleTapCount: 0 })
    }, TAP_RESET_MS)

    const remaining = UNLOCK_TAPS_REQUIRED - nextCount
    if (remaining <= 3) {
      showToast(developerOptions.tapsRemaining(remaining))
    }
  }

  getMenuItems = () => {
    const items = [
      { ...menuItems.reminders, component: 'Reminders' },
      { ...menuItems.nfpSettings, component: 'NfpSettings' },
      { ...menuItems.cycleProfile, component: 'CycleProfile' },
      { ...menuItems.dataManagement, component: 'DataManagement' },
      { ...menuItems.password, component: 'Password' },
    ]

    if (this.state.unlocked) {
      items.push({
        ...menuItems.developerOptions,
        component: 'DeveloperOptions',
      })
    }

    return items
  }

  render() {
    const menu = this.getMenuItems()

    return (
      <AppPage>
        <AppText onPress={this.onTitlePress} style={styles.title}>
          {settingsLabels.title}
        </AppText>
        {menu.map((menuItem, i) => {
          const last = menu.length === i + 1
          return (
            <MenuItem item={menuItem} key={menuItem.component} last={last} />
          )
        })}
      </AppPage>
    )
  }
}

const styles = StyleSheet.create({
  title: {
    ...Typography.title,
  },
})
