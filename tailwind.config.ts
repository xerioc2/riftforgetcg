import type { Config } from 'tailwindcss';

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#101418',
        panel: '#171c22',
        line: '#2b333d',
        forge: '#d8b05d',
        mint: '#6fd3b6',
        ember: '#e56c4f',
      },
      boxShadow: {
        glow: '0 18px 60px rgba(0, 0, 0, 0.32)',
      },
    },
  },
  plugins: [],
} satisfies Config;
