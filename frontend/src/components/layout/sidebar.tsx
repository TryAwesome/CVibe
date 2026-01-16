"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import {
  LayoutDashboard,
  MessageSquare,
  FileText,
  Briefcase,
  LogOut,
  UserCircle,
  TrendingUp,
  Users,
  ShieldCheck,
  Settings
} from "lucide-react";
import { ModeToggle } from "@/components/theme-toggle";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/contexts/auth-context";

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
  const { user, logout } = useAuth();
  // Check admin status from user role (backend returns ROLE_ADMIN or ROLE_USER)
  const isAdmin = user?.role === 'ROLE_ADMIN';

  const handleLogout = async () => {
    await logout();
  };

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
      <div className="border-t p-4 flex items-center gap-2">
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="flex-1 flex items-center justify-start gap-3 h-auto p-2 px-3 hover:bg-accent">
                    <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary/10 text-primary shrink-0">
                        <UserCircle className="h-5 w-5" />
                    </div>
                    <div className="flex-1 overflow-hidden text-left">
                        <p className="truncate text-sm font-medium">{user?.nickname || 'User'}</p>
                        <p className="truncate text-xs text-muted-foreground">{user?.email || ''}</p>
                    </div>
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56" forceMount>
                <DropdownMenuLabel>My Account</DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild>
                    <Link href="/settings" className="cursor-pointer">
                        <Settings className="mr-2 h-4 w-4" />
                        Settings
                    </Link>
                </DropdownMenuItem>
                {isAdmin && (
                    <DropdownMenuItem asChild>
                        <Link href="/admin" className="cursor-pointer">
                            <ShieldCheck className="mr-2 h-4 w-4" />
                            Admin Dashboard
                        </Link>
                    </DropdownMenuItem>
                )}
                <DropdownMenuSeparator />
                <DropdownMenuItem 
                    className="text-destructive focus:text-destructive cursor-pointer"
                    onClick={handleLogout}
                >
                    <LogOut className="mr-2 h-4 w-4" />
                    Log out
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
        <ModeToggle />
      </div>
    </div>
  );
}
