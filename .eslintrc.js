module.exports = {
  root: true,
  extends: ['@react-native-community'],
  parser: '@babel/eslint-parser',
  parserOptions: {
    requireConfigFile: false,
    babelOptions: {
      presets: ['module:metro-react-native-babel-preset'],
    },
  },
  env: {
    es6: true,
    node: true,
    mocha: true,
  },
  rules: {
    /* formatting is left to prettier, which runs on commit via pretty-quick */
    'prettier/prettier': 'off',
    'comma-dangle': 'off',
    curly: 'off',
    'eol-last': 'off',
    'jsx-quotes': 'off',
    'keyword-spacing': 'off',
    quotes: 'off',
    semi: 'off',
    'space-unary-ops': 'off',

    'no-shadow': 'off',
    'react-native/no-inline-styles': 'off',
  },
  overrides: [
    {
      /* the suite runs on mocha + chai, so the jest plugin bundled with
       * @react-native-community misreads assertions like expect(x).to.be.true() */
      files: ['test/**/*.js'],
      rules: {
        'jest/valid-expect': 'off',
      },
    },
  ],
}
