import { Sidebar } from "@/components/layout/sidebar";
import { Header } from "@/components/layout/header";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen bg-muted/40">
      {/* Sidebar - Hidden on mobile, typically controlled by a Sheet/Drawer on mobile */}
      <aside className="hidden w-64 flex-col border-r bg-background md:flex fixed inset-y-0 z-50">
        <Sidebar />
      </aside>

      {/* Main Content */}
      <main className="flex-1 md:ml-64 transition-all duration-300 ease-in-out">
        <div className="flex h-screen flex-col">
            <Header />
            
            {/* Page Content */}
            <div className="flex-1 overflow-hidden">
                {children}
            </div>
        </div>
      </main>
    </div>
  );
}
