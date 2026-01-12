"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { FileText, Clock, Eye } from "lucide-react";

const mockVersions = [
  { id: 1, version: "v1.0", date: "2024-01-10 14:30", type: "Original Upload" },
  { id: 2, version: "v1.1", date: "2024-01-11 09:15", type: "AI Polished" },
  { id: 3, version: "v2.0", date: "2024-01-12 16:45", type: "Tailored for Google" },
];

export function ResumeView() {
  return (
    <div className="grid gap-6 md:grid-cols-3 h-full animate-in fade-in slide-in-from-bottom-4 duration-500">
      {/* Version History List */}
      <Card className="md:col-span-1 h-fit">
        <CardHeader>
          <CardTitle className="text-lg">Version History</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4">
          {mockVersions.map((v) => (
            <div
              key={v.id}
              className="flex items-center justify-between p-3 rounded-lg border bg-muted/50 hover:bg-muted transition-colors cursor-pointer"
            >
              <div className="flex items-center gap-3">
                <div className="bg-background p-2 rounded-md border">
                    <FileText className="h-5 w-5 text-primary" />
                </div>
                <div>
                  <p className="font-medium">{v.version}</p>
                  <p className="text-xs text-muted-foreground flex items-center gap-1">
                    <Clock className="h-3 w-3" /> {v.date}
                  </p>
                </div>
              </div>
              <Badge variant="outline">{v.type}</Badge>
            </div>
          ))}
          <Button className="w-full mt-2" variant="outline">Upload New Version</Button>
        </CardContent>
      </Card>

      {/* PDF Preview Placeholder */}
      <Card className="md:col-span-2 min-h-[500px] flex flex-col">
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-lg">Preview: v2.0</CardTitle>
          <Button size="sm" variant="ghost">
            <Eye className="h-4 w-4 mr-2" /> Full Screen
          </Button>
        </CardHeader>
        <CardContent className="flex-1 bg-muted/20 m-6 rounded-xl border-2 border-dashed border-muted-foreground/20 flex items-center justify-center">
            <div className="text-center text-muted-foreground">
                <FileText className="h-16 w-16 mx-auto mb-4 opacity-50" />
                <p>PDF Preview Placeholder</p>
                <p className="text-sm opacity-60">resume_v2.0_google.pdf</p>
            </div>
        </CardContent>
      </Card>
    </div>
  );
}
