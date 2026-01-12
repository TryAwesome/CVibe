"use client";

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Briefcase, Building2, MapPin, ArrowUpRight } from "lucide-react";

const mockJobs = [
  {
    id: 1,
    title: "Senior Frontend Engineer",
    company: "Netflix",
    location: "Remote / Los Gatos",
    match: 98,
    tags: ["React", "TypeScript", "Performance"],
    posted: "2 days ago",
  },
  {
    id: 2,
    title: "Full Stack Developer",
    company: "Stripe",
    location: "San Francisco, CA",
    match: 94,
    tags: ["Node.js", "React", "Payments"],
    posted: "1 day ago",
  },
  {
    id: 3,
    title: "Software Engineer, Product",
    company: "Notion",
    location: "New York, NY",
    match: 89,
    tags: ["Electron", "Collaboration", "Product"],
    posted: "4 hours ago",
  },
];

export function JobsView() {
  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3 animate-in fade-in slide-in-from-bottom-4 duration-500">
      {mockJobs.map((job) => (
        <Card key={job.id} className="group hover:border-primary/50 transition-colors cursor-pointer">
          <CardHeader>
            <div className="flex justify-between items-start">
                <div className="space-y-1">
                    <CardTitle className="text-lg group-hover:text-primary transition-colors">
                        {job.title}
                    </CardTitle>
                    <CardDescription className="flex items-center gap-1">
                        <Building2 className="h-3 w-3" /> {job.company}
                    </CardDescription>
                </div>
                <Badge variant={job.match > 90 ? "default" : "secondary"} className="text-xs">
                    {job.match}% Match
                </Badge>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <MapPin className="h-3 w-3" /> {job.location}
                    <span className="mx-1">•</span>
                    <span>{job.posted}</span>
                </div>
                <div className="flex flex-wrap gap-2">
                    {job.tags.map(tag => (
                        <Badge key={tag} variant="outline" className="text-[10px] px-2 py-0 h-5">
                            {tag}
                        </Badge>
                    ))}
                </div>
                <Button variant="ghost" className="w-full text-xs group-hover:bg-primary/10">
                    View Details <ArrowUpRight className="h-3 w-3 ml-2" />
                </Button>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
