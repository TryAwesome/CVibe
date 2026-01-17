import type { Metadata } from "next";
import { Inter, Zen_Dots } from "next/font/google";
import "./globals.css";
import { cn } from "@/lib/utils";
import { Toaster } from "sonner";

const inter = Inter({ subsets: ["latin"], variable: "--font-inter" });
const zenDots = Zen_Dots({ weight: "400", subsets: ["latin"], variable: "--font-zen-dots" });

export const metadata: Metadata = {
  title: "CVibe",
  description: "AI-driven career optimization",
};

import { ThemeProvider } from "@/components/theme-provider";
import { AuthProvider } from "@/lib/contexts/auth-context";

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={cn("min-h-screen bg-background font-sans antialiased", inter.variable, zenDots.variable)}>
        <ThemeProvider
            attribute="class"
            defaultTheme="system"
            enableSystem
            disableTransitionOnChange
          >
            <AuthProvider>
              {children}
              <Toaster richColors position="top-right" />
            </AuthProvider>
          </ThemeProvider>
      </body>
    </html>
  );
}
