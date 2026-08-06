import js from '@eslint/js'
import prettier from 'eslint-config-prettier'
import vue from 'eslint-plugin-vue'
import tseslint from 'typescript-eslint'

export default [
  {
    ignores: ['dist', 'coverage', 'playwright-report', 'test-results'],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...vue.configs['flat/recommended'],
  prettier,
  {
    files: ['**/*.{ts,vue}'],
    languageOptions: {
      parserOptions: {
        parser: tseslint.parser,
      },
    },
    rules: {
      'vue/multi-word-component-names': 'off',
      // .ts 는 typescript-eslint 가 이미 꺼두는 규칙이다. .vue 에도 같이 적용해
      // DOM 전역(HTMLVideoElement, MediaStream 등)이 미정의로 잡히지 않게 한다 —
      // 미정의 식별자는 vue-tsc 가 검사한다.
      'no-undef': 'off',
    },
  },
]
