"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import {
  LayoutDashboard,
  MessageSquare,
  FileText,
  Briefcase,
  Settings,
  LogOut,
  UserCircle,
  TrendingUp,
  Users
} from "lucide-react";
import { ModeToggle } from "@/components/theme-toggle";

const sidebarItems = [
  {
    title: "Dashboard",
    href: "/dashboard",
    icon: LayoutDashboard,
  },
  {
    title: "Interview",
    href: "/interview",
    icon: MessageSquare,
  },
  {
    title: "Career Growth",
    href: "/growth",
    icon: TrendingUp,
  },
  {
    title: "Resume Builder",
    href: "/resume-builder",
    icon: FileText,
  },
  {
    title: "Job Recommendations",
    href: "/jobs",
    icon: Briefcase,
  },
  {
    title: "Mock Interview",
    href: "/mock-interview",
    icon: UserCircle, 
  },
  {
    title: "Community",
    href: "/community",
    icon: Users,
  },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <div className="flex h-screen w-64 flex-col border-r bg-card text-card-foreground">
      {/* Logo Area */}
      <div className="flex h-16 items-center border-b px-6">
        <h1 className="text-2xl font-[family-name:var(--font-zen-dots)] text-primary">CVibe</h1>
      </div>

      {/* Navigation Links */}
      <div className="flex-1 overflow-y-auto py-4">
        <nav className="grid gap-1 px-2">
          {sidebarItems.map((item, index) => {
            const isActive = pathname.startsWith(item.href);
            return (
              <Link
                key={index}
                href={item.href}
                className={cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all hover:bg-accent hover:text-accent-foreground",
                  isActive
                    ? "bg-primary text-primary-foreground shadow-sm"
                    : "text-muted-foreground"
                )}
              >
                <item.icon className="h-5 w-5" />
                {item.title}
              </Link>
            );
          })}
        </nav>
      </div>

      {/* User Profile / Footer */}
      <div className="border-t p-4">
        <Link href="/settings" className="flex items-center gap-3 rounded-lg bg-accent/50 p-3 hover:bg-accent transition-colors cursor-pointer">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10 text-primary">
                <UserCircle className="h-6 w-6" />
            </div>
            <div className="flex-1 overflow-hidden">
                <p className="truncate text-sm font-medium">Demo User</p>
                <p className="truncate text-xs text-muted-foreground">demo@cvibe.ai</p>
            </div>
            <ModeToggle />
            <button className="text-muted-foreground hover:text-destructive" onClick={(e) => {
                e.preventDefault(); // Prevent navigation when clicking logout
                // Handle logout logic
            }}>
                <LogOut className="h-5 w-5" />
            </button>
        </Link>
      </div>
    </div>
  );
}
