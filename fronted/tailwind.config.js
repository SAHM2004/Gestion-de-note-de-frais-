/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        "on-surface": "#191c1d",
        "background": "#f8f9fa",
        "secondary-fixed": "#c1e8ff",
        "on-secondary": "#ffffff",
        "secondary-container": "#87d6fe",
        "on-background": "#191c1d",
        "surface-bright": "#f8f9fa",
        "outline-variant": "#c3c6cf",
        "primary-container": "#1a3c5e",
        "on-secondary-fixed": "#001e2b",
        "surface-variant": "#e1e3e4",
        "surface-tint": "#416084",
        "surface-container-lowest": "#ffffff",
        "on-primary-container": "#87a7ce",
        "secondary-fixed-dim": "#81d0f8",
        "inverse-on-surface": "#f0f1f2",
        "inverse-surface": "#2e3132",
        "on-error": "#ffffff",
        "on-tertiary": "#ffffff",
        "on-secondary-container": "#005d7c",
        "on-tertiary-fixed": "#281800",
        "surface-container-highest": "#e1e3e4",
        "secondary": "#006687",
        "on-primary": "#ffffff",
        "surface-container-high": "#e7e8e9",
        "error-container": "#ffdad6",
        "tertiary-container": "#513500",
        "primary-fixed-dim": "#a9c9f2",
        "on-tertiary-fixed-variant": "#5f410b",
        "surface": "#f8f9fa",
        "tertiary-fixed": "#ffddb0",
        "surface-container-low": "#f3f4f5",
        "outline": "#73777f",
        "primary-fixed": "#d1e4ff",
        "on-primary-fixed-variant": "#28496b",
        "on-surface-variant": "#43474e",
        "tertiary-fixed-dim": "#ecbf7e",
        "on-error-container": "#93000a",
        "surface-container": "#edeeef",
        "tertiary": "#352100",
        "on-tertiary-container": "#c79e60",
        "error": "#ba1a1a",
        "surface-dim": "#d9dadb",
        "inverse-primary": "#a9c9f2",
        "on-secondary-fixed-variant": "#004d67",
        "primary": "#002645",
        "on-primary-fixed": "#001d36"
      },
      borderRadius: {
        "DEFAULT": "0.25rem",
        "lg": "0.5rem",
        "xl": "0.75rem",
        "full": "9999px"
      },
      spacing: {
        "base": "8px",
        "card-gap": "20px",
        "container-padding": "24px",
        "sidebar-width": "280px",
        "gutter": "16px"
      },
      fontFamily: {
        "headline-lg": ["Inter"],
        "headline-md": ["Inter"],
        "headline-sm": ["Inter"],
        "label-md": ["Inter"],
        "body-md": ["Inter"],
        "body-lg": ["Inter"]
      },
      fontSize: {
        "headline-lg": ["32px", {"lineHeight": "1.2", "letterSpacing": "-0.02em", "fontWeight": "700"}],
        "headline-md": ["24px", {"lineHeight": "1.3", "fontWeight": "600"}],
        "headline-sm": ["18px", {"lineHeight": "1.4", "fontWeight": "600"}],
        "label-md": ["12px", {"lineHeight": "1", "letterSpacing": "0.05em", "fontWeight": "600"}],
        "body-md": ["14px", {"lineHeight": "1.5", "fontWeight": "400"}],
        "body-lg": ["16px", {"lineHeight": "1.6", "fontWeight": "400"}]
      }
    }
  },
  plugins: [],
};
