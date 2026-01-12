"use client";

import { Bell } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Badge } from "@/components/ui/badge";

export function Header() {
  return (
    <header className="sticky top-0 z-30 flex h-16 items-center gap-4 border-b bg-background px-6 shadow-sm">
      <div className="flex-1">
        {/* Breadcrumb or Title placeholder */}
        {/* <h2 className="text-lg font-semibold">Dashboard</h2> */}
      </div>
      
      {/* Notification Bell */}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon" className="relative">
            <Bell className="h-5 w-5" />
            <span className="absolute top-2 right-2 h-2 w-2 rounded-full bg-red-600 border border-background" />
            <span className="sr-only">Notifications</span>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-80">
            <DropdownMenuLabel>Notifications</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem className="cursor-pointer flex flex-col items-start gap-1 p-3">
                <span className="font-medium text-sm">New Comment</span>
                <span className="text-xs text-muted-foreground">Alice commented: "This is super helpful!"</span>
                <span className="text-[10px] text-muted-foreground mt-1">2 mins ago</span>
            </DropdownMenuItem>
            <DropdownMenuItem className="cursor-pointer flex flex-col items-start gap-1 p-3">
                <span className="font-medium text-sm">System Update</span>
                <span className="text-xs text-muted-foreground">Your resume analysis is complete.</span>
                <span className="text-[10px] text-muted-foreground mt-1">1 hour ago</span>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem className="justify-center text-primary text-xs">
                View All Notifications
            </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </header>
  );
}
