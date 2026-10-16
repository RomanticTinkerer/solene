import React, { Component } from 'react'
import { StyleSheet } from 'react-native'
import PropTypes from 'prop-types'

import AppPage from '../common/app-page'
import AppText from '../common/app-text'
import AppTextInput from '../common/app-text-input'
import Segment from '../common/segment'
import SettingsButton from './common/settings-button'

import {
  cycleProfileObservable,
  saveCycleProfile,
} from '../../local-storage'
import { showToast } from '../helpers/general'
import { Spacing } from '../../styles'
import labels from '../../i18n/en/settings'

const t = labels.cycleProfile

function toField(value) {
  return value === null || value === undefined ? '' : String(value)
}

function parseOptional(value, min, max) {
  const trimmed = String(value).trim()
  if (!trimmed) return { ok: true, value: null }
  const parsed = Number(trimmed.replace(',', '.'))
  if (!Number.isFinite(parsed) || parsed < min || parsed > max) {
    return { ok: false, value: null }
  }
  return { ok: true, value: parsed }
}

function bmiFrom(heightCm, weightKg) {
  if (typeof heightCm !== 'number' || typeof weightKg !== 'number') return null
  if (heightCm <= 0) return null
  const meters = heightCm / 100
  return weightKg / (meters * meters)
}

export default class CycleProfile extends Component {
  constructor(props) {
    super(props)
    const profile = cycleProfileObservable.value || {}
    this.state = {
      age: toField(profile.age),
      heightCm: toField(profile.heightCm),
      weightKg: toField(profile.weightKg),
    }
  }

  save = async (profile) => {
    await saveCycleProfile(profile)
    this.setState({
      age: toField(profile.age),
      heightCm: toField(profile.heightCm),
      weightKg: toField(profile.weightKg),
    })
  }

  onSave = async () => {
    const age = parseOptional(this.state.age, 12, 80)
    const heightCm = parseOptional(this.state.heightCm, 120, 220)
    const weightKg = parseOptional(this.state.weightKg, 30, 200)
    if (!age.ok || !heightCm.ok || !weightKg.ok) {
      showToast(t.invalid)
      return
    }
    await this.save({
      age: age.value,
      heightCm: heightCm.value,
      weightKg: weightKg.value,
    })
    showToast(t.savedToast)
  }

  onClear = async () => {
    await this.save({ age: null, heightCm: null, weightKg: null })
    showToast(t.clearedToast)
  }

  render() {
    const { age, heightCm, weightKg } = this.state
    const parsedHeight = parseOptional(heightCm, 120, 220)
    const parsedWeight = parseOptional(weightKg, 30, 200)
    const bmi =
      parsedHeight.ok && parsedWeight.ok
        ? bmiFrom(parsedHeight.value, parsedWeight.value)
        : null

    return (
      <AppPage title={t.title}>
        <Segment last>
          <AppText>{t.explainer}</AppText>
          <AppText style={styles.fieldLabel}>{t.age}</AppText>
          <AppTextInput
            keyboardType="numeric"
            onChangeText={(value) => this.setState({ age: value })}
            value={age}
          />
          <AppText style={styles.fieldLabel}>{t.height}</AppText>
          <AppTextInput
            keyboardType="numeric"
            onChangeText={(value) => this.setState({ heightCm: value })}
            value={heightCm}
          />
          <AppText style={styles.fieldLabel}>{t.weight}</AppText>
          <AppTextInput
            keyboardType="numeric"
            onChangeText={(value) => this.setState({ weightKg: value })}
            value={weightKg}
          />
          {bmi !== null && (
            <AppText style={styles.bmi}>{t.bmi(bmi.toFixed(1))}</AppText>
          )}
          <SettingsButton onPress={this.onSave}>{t.save}</SettingsButton>
          <SettingsButton onPress={this.onClear} secondary>
            {t.clear}
          </SettingsButton>
        </Segment>
      </AppPage>
    )
  }
}

CycleProfile.propTypes = {
  date: PropTypes.string,
}

const styles = StyleSheet.create({
  bmi: {
    marginTop: Spacing.base,
  },
  fieldLabel: {
    marginTop: Spacing.base,
  },
})
