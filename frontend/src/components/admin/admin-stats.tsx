import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Activity, Users, FileText, Cpu } from "lucide-react";
import { cn } from "@/lib/utils";

export type MetricType = "users" | "jobs" | "tokens" | "health";

interface AdminStatsProps {
  selectedMetric?: MetricType;
  onSelectMetric?: (metric: MetricType) => void;
}

export function AdminStats({ selectedMetric, onSelectMetric }: AdminStatsProps) {
  const handleSelect = (metric: MetricType) => {
    if (onSelectMetric) {
      onSelectMetric(metric);
    }
  };

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
      <Card 
        className={cn("cursor-pointer transition-all hover:shadow-md", selectedMetric === "users" && "border-primary ring-1 ring-primary")}
        onClick={() => handleSelect("users")}
      >
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">Total Users</CardTitle>
          <Users className="h-4 w-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">12,345</div>
          <p className="text-xs text-muted-foreground">+180 from yesterday</p>
        </CardContent>
      </Card>
      <Card 
        className={cn("cursor-pointer transition-all hover:shadow-md", selectedMetric === "jobs" && "border-primary ring-1 ring-primary")}
        onClick={() => handleSelect("jobs")}
      >
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">Active Jobs</CardTitle>
          <FileText className="h-4 w-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">2,543</div>
          <p className="text-xs text-muted-foreground">+201 crawled today</p>
        </CardContent>
      </Card>
      <Card 
        className={cn("cursor-pointer transition-all hover:shadow-md", selectedMetric === "tokens" && "border-primary ring-1 ring-primary")}
        onClick={() => handleSelect("tokens")}
      >
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">AI Usage (Tokens)</CardTitle>
          <Cpu className="h-4 w-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">45.2M</div>
          <p className="text-xs text-muted-foreground">+2.1M this week</p>
        </CardContent>
      </Card>
      <Card 
        className={cn("cursor-pointer transition-all hover:shadow-md", selectedMetric === "health" && "border-primary ring-1 ring-primary")}
        onClick={() => handleSelect("health")}
      >
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">System Health</CardTitle>
          <Activity className="h-4 w-4 text-green-500" />
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-1">
             <div className="flex items-center text-sm">
                <div className="w-2 h-2 rounded-full bg-green-500 mr-2" />
                Biz Service
             </div>
             <div className="flex items-center text-sm">
                <div className="w-2 h-2 rounded-full bg-green-500 mr-2" />
                AI Engine
             </div>
             <div className="flex items-center text-sm">
                <div className="w-2 h-2 rounded-full bg-yellow-500 mr-2" />
                Search Service (Lag)
             </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
