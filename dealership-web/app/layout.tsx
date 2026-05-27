// app/layout.tsx
// Root layout — injects fonts, dark mode script, and TanStack Query provider.

import type { Metadata } from "next";
import { GeistSans } from "geist/font/sans";
import { GeistMono } from "geist/font/mono";
import { Bricolage_Grotesque } from "next/font/google";
import { SkipLink } from "@/components/layout/SkipLink";
import { Providers } from "@/components/layout/Providers";
import { WebVitals } from "@/app/_vitals";
import "./globals.css";

const bricolageGrotesque = Bricolage_Grotesque({
  subsets: ["latin"],
  variable: "--font-bricolage",
  display: "swap",
});

export const metadata: Metadata = {
  title: {
    template: "%s | Aurelio Motors",
    default: "Aurelio Motors — Veículos Seminovos",
  },
  description:
    "Encontre o seu próximo veículo com a Aurelio Motors. Estoque selecionado de seminovos com qualidade garantida.",
};

/**
 * Inline dark mode script.
 * Must be the first <script> in <head> to prevent FOUC.
 */
const darkModeScript = `(function(){try{var t=localStorage.getItem('theme');if(t==='dark'||(t===null&&window.matchMedia('(prefers-color-scheme:dark)').matches)){document.documentElement.classList.add('dark')}}catch(e){}})();`;

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="pt-BR"
      className={`${GeistSans.variable} ${GeistMono.variable} ${bricolageGrotesque.variable} h-full`}
      suppressHydrationWarning
    >
      <head>
        {/* Dark mode FOUC prevention — must be inline, must be first */}
        <script dangerouslySetInnerHTML={{ __html: darkModeScript }} />
      </head>
      <body className="flex min-h-full flex-col bg-background text-foreground antialiased">
        <SkipLink />
        <Providers>
          <WebVitals />
          {children}
        </Providers>
      </body>
    </html>
  );
}
