"use client";

import { useState } from "react";
import { UserAuditView } from "./user-audit-view";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Search } from "lucide-react";

// Mock Data
const MOCK_USERS = [
  { id: "1", name: "Alice Johnson", email: "alice@example.com", role: "User", status: "Active" as const, joinedAt: "2024-01-10", avatarUrl: "" },
  { id: "2", name: "Bob Smith", email: "bob@example.com", role: "Admin", status: "Active" as const, joinedAt: "2023-12-05", avatarUrl: "" },
  { id: "3", name: "Charlie Brown", email: "charlie@gmail.com", role: "User", status: "Suspended" as const, joinedAt: "2024-01-15", avatarUrl: "" },
  { id: "4", name: "Diana Prince", email: "diana@amazon.com", role: "User", status: "Active" as const, joinedAt: "2023-11-20", avatarUrl: "" },
  { id: "5", name: "Evan Wright", email: "evan@tech.io", role: "User", status: "Active" as const, joinedAt: "2024-02-01", avatarUrl: "" },
];

export function UserManagement() {
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");

  const filteredUsers = MOCK_USERS.filter(user =>
    user.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    user.email.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const selectedUser = MOCK_USERS.find(u => u.id === selectedUserId) || null;

  return (
    <div className="flex h-[800px] border rounded-lg overflow-hidden">
      {/* Left List */}
      <div className={`${selectedUser ? 'w-1/3 hidden md:block' : 'w-full'} flex flex-col border-r bg-background`}>
        <div className="p-4 border-b">
            <div className="relative">
                <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input
                    placeholder="Search users..."
                    className="pl-8"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                />
            </div>
        </div>
        <div className="flex-1 overflow-y-auto">
            {filteredUsers.map((user) => (
                <div
                    key={user.id}
                    className={`flex items-center gap-3 p-4 cursor-pointer hover:bg-muted/50 transition-colors ${selectedUserId === user.id ? 'bg-muted' : ''}`}
                    onClick={() => setSelectedUserId(user.id)}
                >
                    <Avatar>
                        <AvatarImage src={user.avatarUrl} />
                        <AvatarFallback>{user.name.substring(0, 2).toUpperCase()}</AvatarFallback>
                    </Avatar>
                    <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between">
                            <span className="font-medium truncate">{user.name}</span>
                            {user.status === "Suspended" && (
                                <Badge variant="destructive" className="text-[10px] h-5 px-1">Suspended</Badge>
                            )}
                        </div>
                        <p className="text-sm text-muted-foreground truncate">{user.email}</p>
                    </div>
                </div>
            ))}
            {filteredUsers.length === 0 && (
                <div className="p-8 text-center text-muted-foreground">
                    No users found
                </div>
            )}
        </div>
      </div>

      {/* Right Detail */}
      <div className={`${selectedUser ? 'w-full md:w-2/3' : 'hidden'} bg-background border-l`}>
         <UserAuditView user={selectedUser} onClose={() => setSelectedUserId(null)} />
      </div>
    </div>
  );
}
