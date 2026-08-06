import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          50: "#eef8f2",
          100: "#d6eee0",
          200: "#b0dcc4",
          300: "#82c3a1",
          400: "#54a67d",
          500: "#2e8b57",
          600: "#267a4c",
          700: "#1f643e",
          800: "#194f32",
          900: "#143e28",
        },
        accent: {
          400: "#f4a460",
          500: "#e8933f",
        },
        surface: {
          DEFAULT: "#faf9f5",
          card: "#ffffff",
        },
      },
      fontFamily: {
        sans: [
          "-apple-system",
          "BlinkMacSystemFont",
          "Segoe UI",
          "Roboto",
          "Helvetica Neue",
          "Arial",
          "sans-serif",
        ],
      },
    },
  },
  plugins: [],
};

export default config;
