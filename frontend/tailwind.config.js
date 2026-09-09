/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // 포인트 컬러: 코랄오렌지 (버거/식욕 + 배달앱 톤)
        brand: {
          DEFAULT: '#FF5A1F',
          50: '#FFF3EE',
          100: '#FFE3D6',
          200: '#FFC4AD',
          300: '#FF9E78',
          400: '#FF7A47',
          500: '#FF5A1F',
          600: '#F03E00',
          700: '#C63300',
          800: '#992800',
          900: '#7A2100',
        },
        ink: {
          DEFAULT: '#1A1A1A',
          soft: '#4B4B4B',
          faint: '#8A8A8A',
        },
      },
      fontFamily: {
        sans: [
          'Pretendard',
          'system-ui',
          '-apple-system',
          'Segoe UI',
          'Roboto',
          'Apple SD Gothic Neo',
          'sans-serif',
        ],
      },
      boxShadow: {
        card: '0 2px 12px rgba(0,0,0,0.06)',
        'card-hover': '0 8px 24px rgba(0,0,0,0.10)',
      },
      borderRadius: {
        xl2: '1.25rem',
      },
    },
  },
  plugins: [],
};
