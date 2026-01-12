"use client";

import { useState } from "react";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription
} from "@/components/ui/card";
import { FileText, User, Briefcase, ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";
import { ResumeView } from "@/components/dashboard/resume-view";
import { ProfileView } from "@/components/dashboard/profile-view";
import { JobsView } from "@/components/dashboard/jobs-view";

type TabType = "resume" | "profile" | "jobs" | null;

export default function DashboardPage() {
  const [activeTab, setActiveTab] = useState<TabType>(null);

  const toggleTab = (tab: TabType) => {
    setActiveTab((current) => (current === tab ? null : tab));
  };

  return (
    <div className="space-y-8 p-8 pt-6 h-full overflow-y-auto">
      {/* Top Navigation Cards */}
      <div className="grid gap-6 md:grid-cols-3">
        {/* Resume History Card */}
        <Card 
            className={cn(
                "cursor-pointer transition-all hover:border-primary hover:shadow-md",
                activeTab === "resume" ? "border-primary ring-1 ring-primary" : ""
            )}
            onClick={() => toggleTab("resume")}
        >
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-medium">Resume History</CardTitle>
            <FileText className={cn("h-5 w-5", activeTab === "resume" ? "text-primary" : "text-muted-foreground")} />
          </CardHeader>
          <CardContent>
            <div className="text-sm text-muted-foreground mt-2">
              View version history and preview PDF.
            </div>
            <div className="mt-4 flex justify-center">
                <ChevronDown className={cn("h-5 w-5 transition-transform duration-300", activeTab === "resume" ? "rotate-180" : "")} />
            </div>
          </CardContent>
        </Card>

        {/* Profile Database Card */}
        <Card 
            className={cn(
                "cursor-pointer transition-all hover:border-primary hover:shadow-md",
                activeTab === "profile" ? "border-primary ring-1 ring-primary" : ""
            )}
            onClick={() => toggleTab("profile")}
        >
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-medium">Profile Database</CardTitle>
            <User className={cn("h-5 w-5", activeTab === "profile" ? "text-primary" : "text-muted-foreground")} />
          </CardHeader>
          <CardContent>
             <div className="text-sm text-muted-foreground mt-2">
              Manage your education, experience, skills...
            </div>
            <div className="mt-4 flex justify-center">
                <ChevronDown className={cn("h-5 w-5 transition-transform duration-300", activeTab === "profile" ? "rotate-180" : "")} />
            </div>
          </CardContent>
        </Card>

        {/* Matched Jobs Card */}
        <Card 
            className={cn(
                "cursor-pointer transition-all hover:border-primary hover:shadow-md",
                activeTab === "jobs" ? "border-primary ring-1 ring-primary" : ""
            )}
            onClick={() => toggleTab("jobs")}
        >
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-medium">Matched Jobs</CardTitle>
            <Briefcase className={cn("h-5 w-5", activeTab === "jobs" ? "text-primary" : "text-muted-foreground")} />
          </CardHeader>
          <CardContent>
            <div className="text-sm text-muted-foreground mt-2">
              Browse jobs tailored to your profile.
            </div>
            <div className="mt-4 flex justify-center">
                <ChevronDown className={cn("h-5 w-5 transition-transform duration-300", activeTab === "jobs" ? "rotate-180" : "")} />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Detail View Area */}
      <div className="min-h-[400px] rounded-xl border bg-card p-6 shadow-sm transition-all relative overflow-hidden">
        {!activeTab && (
            <div className="absolute inset-0 flex items-center justify-center text-muted-foreground">
                <p>Select a module above to view details</p>
            </div>
        )}
        
        {activeTab === "resume" && <ResumeView />}
        {activeTab === "profile" && <ProfileView />}
        {activeTab === "jobs" && <JobsView />}
      </div>
    </div>
  );
}