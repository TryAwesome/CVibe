import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ShieldAlert, FileText, MessageSquare, Ban, Users } from "lucide-react";

interface User {
  id: string;
  name: string;
  email: string;
  role: string;
  status: "Active" | "Suspended";
  avatarUrl?: string;
  joinedAt: string;
}

interface UserAuditViewProps {
  user: User | null;
  onClose: () => void;
}

export function UserAuditView({ user, onClose }: UserAuditViewProps) {
  if (!user) {
    return (
      <div className="flex h-full items-center justify-center p-8 text-muted-foreground border-l min-h-[400px]">
        <div className="text-center">
          <Users className="mx-auto h-12 w-12 opacity-20" />
          <p className="mt-2">Select a user to view audit trail</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full border-l bg-card/50">
      <div className="p-6 border-b flex items-start justify-between">
        <div className="flex items-center gap-4">
          <Avatar className="h-16 w-16">
            <AvatarImage src={user.avatarUrl} />
            <AvatarFallback>{user.name.substring(0, 2).toUpperCase()}</AvatarFallback>
          </Avatar>
          <div>
            <h2 className="text-2xl font-bold">{user.name}</h2>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
               <span>{user.email}</span>
               <Badge variant={user.status === "Active" ? "default" : "destructive"}>
                 {user.status}
               </Badge>
               <Badge variant="outline">{user.role}</Badge>
            </div>
            <p className="text-xs text-muted-foreground mt-1">Joined: {user.joinedAt}</p>
          </div>
        </div>
        <Button variant="ghost" onClick={onClose}>Close</Button>
      </div>

      <div className="flex-1 overflow-y-auto p-6 space-y-6">
        {/* Actions */}
        <div className="grid grid-cols-2 gap-4">
             <Button variant="destructive" className="w-full">
                <Ban className="mr-2 h-4 w-4" /> Suspend User
             </Button>
             <Button variant="outline" className="w-full">
                <ShieldAlert className="mr-2 h-4 w-4" /> Reset Password
             </Button>
        </div>

        {/* Resume History */}
        <Card>
            <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center">
                    <FileText className="mr-2 h-4 w-4" /> Resume History
                </CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
                {[1, 2, 3].map((i) => (
                    <div key={i} className="flex items-center justify-between p-2 border rounded-md text-sm">
                        <div className="flex items-center">
                            <span className="font-medium">Resume_v{4-i}.pdf</span>
                            <span className="ml-2 text-xs text-muted-foreground">Uploaded 2 days ago</span>
                        </div>
                        <Button variant="ghost" size="sm">View</Button>
                    </div>
                ))}
            </CardContent>
        </Card>

        {/* Community Activity */}
        <Card>
            <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center">
                    <MessageSquare className="mr-2 h-4 w-4" /> Community Activity
                </CardTitle>
            </CardHeader>
             <CardContent className="space-y-4">
                <div className="border-b pb-2">
                    <p className="text-sm font-medium">"How to handle behavioral interview questions?"</p>
                    <p className="text-xs text-muted-foreground mt-1">Posted in General • 5 hours ago</p>
                </div>
                <div className="border-b pb-2">
                    <p className="text-sm font-medium">Commented on "Big Tech Layoffs"</p>
                    <p className="text-xs text-muted-foreground mt-1">"I think it will stabilize soon..." • 1 day ago</p>
                </div>
             </CardContent>
        </Card>
      </div>
    </div>
  );
}
