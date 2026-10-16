import React from 'react'
import { Image, StyleSheet, View } from 'react-native'
import PropTypes from 'prop-types'

import AppText from '../common/app-text'
import { Colors, Fonts, Sizes, Spacing } from '../../styles'

const ForecastCard = ({ illustration, title, body, meta }) => {
  return (
    <View style={styles.card}>
      <Image source={illustration} style={styles.image} />
      <View style={styles.copy}>
        <AppText style={styles.title}>{title}</AppText>
        <AppText style={styles.body}>{body}</AppText>
        {!!meta && <AppText style={styles.meta}>{meta}</AppText>}
      </View>
    </View>
  )
}

ForecastCard.propTypes = {
  body: PropTypes.string.isRequired,
  illustration: PropTypes.oneOfType([PropTypes.number, PropTypes.object])
    .isRequired,
  meta: PropTypes.string,
  title: PropTypes.string.isRequired,
}

const styles = StyleSheet.create({
  body: {
    color: Colors.greyDark,
    fontSize: Sizes.small,
    marginTop: Spacing.tiny,
  },
  card: {
    backgroundColor: Colors.turquoiseLight,
    borderRadius: 16,
    flexDirection: 'row',
    marginTop: Spacing.small,
    padding: Spacing.small,
  },
  copy: {
    flex: 1,
    justifyContent: 'center',
    paddingLeft: Spacing.small,
  },
  image: {
    height: 56,
    width: 56,
  },
  meta: {
    color: Colors.purple,
    fontFamily: Fonts.bold,
    fontSize: Sizes.footnote,
    marginTop: Spacing.tiny,
    textTransform: 'uppercase',
  },
  title: {
    color: Colors.purple,
    fontFamily: Fonts.bold,
    fontSize: Sizes.base,
  },
})

export default ForecastCard
