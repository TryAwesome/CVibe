"use client";

import { useState } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { AdminStats, MetricType } from "@/components/admin/admin-stats";
import { UserManagement } from "@/components/admin/user-management";
import { OverviewChart } from "@/components/admin/overview-chart";
import { ResumeTemplatesView } from "@/components/admin/resume-templates-view";
import { InterviewQuestionsView } from "@/components/admin/interview-questions-view";

export default function AdminDashboardPage() {
  const [selectedMetric, setSelectedMetric] = useState<MetricType>("users");

  return (
    <div className="flex-1 space-y-4">
      <div className="flex items-center justify-between space-y-2">
        <h2 className="text-3xl font-bold tracking-tight">Dashboard</h2>
      </div>
      <Tabs defaultValue="overview" className="space-y-4">
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="users">User Audit</TabsTrigger>
          <TabsTrigger value="templates">Resume Templates</TabsTrigger>
          <TabsTrigger value="interviews">Interview Bank</TabsTrigger>
        </TabsList>
        <TabsContent value="overview" className="space-y-4">
          <AdminStats selectedMetric={selectedMetric} onSelectMetric={setSelectedMetric} />
          <OverviewChart type={selectedMetric} />
        </TabsContent>
        <TabsContent value="users" className="space-y-4">
          <UserManagement />
        </TabsContent>
        <TabsContent value="templates" className="space-y-4">
          <ResumeTemplatesView />
        </TabsContent>
        <TabsContent value="interviews" className="space-y-4">
          <InterviewQuestionsView />
        </TabsContent>
      </Tabs>
    </div>
  );
}
