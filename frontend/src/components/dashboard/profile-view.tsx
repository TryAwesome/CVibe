"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Plus, Pencil, GraduationCap, Briefcase, Trophy } from "lucide-react";

export function ProfileView() {
  return (
    <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
      {/* Education Module */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
            <div className="flex items-center gap-2">
                <GraduationCap className="h-5 w-5 text-primary" />
                <CardTitle className="text-lg">Education</CardTitle>
            </div>
            <Button size="icon" variant="ghost"><Plus className="h-4 w-4" /></Button>
        </CardHeader>
        <CardContent className="grid gap-4">
            <div className="p-4 rounded-lg border bg-muted/20 flex justify-between items-start">
                <div>
                    <h4 className="font-semibold">Stanford University</h4>
                    <p className="text-sm text-muted-foreground">Master of Computer Science</p>
                    <p className="text-xs text-muted-foreground mt-1">2022 - 2024</p>
                </div>
                <Button size="icon" variant="ghost" className="h-8 w-8"><Pencil className="h-3 w-3" /></Button>
            </div>
        </CardContent>
      </Card>

      {/* Internships Module */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
            <div className="flex items-center gap-2">
                <Briefcase className="h-5 w-5 text-primary" />
                <CardTitle className="text-lg">Internships & Work Experience</CardTitle>
            </div>
            <Button size="icon" variant="ghost"><Plus className="h-4 w-4" /></Button>
        </CardHeader>
        <CardContent className="grid gap-4">
             <div className="p-4 rounded-lg border bg-muted/20 flex justify-between items-start">
                <div>
                    <h4 className="font-semibold">Google</h4>
                    <p className="text-sm text-muted-foreground">Software Engineering Intern</p>
                    <p className="text-xs text-muted-foreground mt-1">Summer 2023</p>
                    <ul className="list-disc list-inside text-sm text-muted-foreground mt-2 pl-2">
                        <li>Optimized distributed storage system latency by 20%.</li>
                        <li>Implemented new React components for internal dashboard.</li>
                    </ul>
                </div>
                <Button size="icon" variant="ghost" className="h-8 w-8"><Pencil className="h-3 w-3" /></Button>
            </div>
        </CardContent>
      </Card>

      {/* Awards Module */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
            <div className="flex items-center gap-2">
                <Trophy className="h-5 w-5 text-primary" />
                <CardTitle className="text-lg">Awards & Achievements</CardTitle>
            </div>
            <Button size="icon" variant="ghost"><Plus className="h-4 w-4" /></Button>
        </CardHeader>
        <CardContent className="grid gap-4">
            <div className="p-4 rounded-lg border bg-muted/20 flex justify-between items-center">
                <div>
                    <h4 className="font-semibold">ACM ICPC Regional Gold Medal</h4>
                    <p className="text-sm text-muted-foreground">2022</p>
                </div>
                <Button size="icon" variant="ghost" className="h-8 w-8"><Pencil className="h-3 w-3" /></Button>
            </div>
        </CardContent>
      </Card>
    </div>
  );
}
