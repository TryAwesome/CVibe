import type { Metadata } from "next";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { ModeToggle } from "@/components/theme-toggle";

export const metadata: Metadata = {
  title: "CVibe Admin",
  description: "Administrative Console",
};

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col min-h-screen">
       <header className="sticky top-0 z-50 w-full border-b border-border/40 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
          <div className="container flex h-14 max-w-screen-2xl items-center">
            <div className="mr-4 flex">
              <Link className="mr-6 flex items-center space-x-2" href="/admin">
                <span className="font-[family-name:var(--font-zen-dots)] text-xl sm:inline-block">
                  CVibe <span className="text-primary font-sans text-sm align-super">Admin</span>
                </span>
              </Link>
            </div>
            <div className="flex flex-1 items-center justify-between space-x-2 md:justify-end">
              <nav className="flex items-center space-x-2">
                <Button variant="ghost" asChild>
                  <Link href="/dashboard">Back to App</Link>
                </Button>
                <ModeToggle />
              </nav>
            </div>
          </div>
        </header>
        <main className="flex-1 space-y-4 p-8 pt-6 container max-w-screen-2xl">
            {children}
        </main>
    </div>
  );
}