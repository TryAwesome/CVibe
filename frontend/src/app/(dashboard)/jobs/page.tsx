"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Briefcase, MapPin, DollarSign, Clock, RefreshCw, Filter, ExternalLink, CheckCircle2, XCircle, Search } from "lucide-react";
import { cn } from "@/lib/utils";

// Mock Data
const MOCK_JOBS = [
  {
    id: 1,
    title: "Senior Frontend Engineer",
    company: "Stripe",
    location: "San Francisco (Hybrid)",
    salary: "$180k - $240k",
    type: "Full-time",
    posted: "2 hours ago",
    matchScore: 98,
    matchReason: "Strong match with your React and System Design experience.",
    skills: ["React", "TypeScript", "GraphQL", "AWS"],
    missingSkills: [],
    logo: "bg-indigo-500"
  },
  {
    id: 2,
    title: "Product Engineer",
    company: "Notion",
    location: "New York / Remote",
    salary: "$160k - $210k",
    type: "Full-time",
    posted: "5 hours ago",
    matchScore: 92,
    matchReason: "Your internship at Google aligns well with their product focus.",
    skills: ["Next.js", "Electron", "Collaboration"],
    missingSkills: ["Rust"],
    logo: "bg-stone-800"
  },
  {
    id: 3,
    title: "Backend Developer",
    company: "Uber",
    location: "Seattle, WA",
    salary: "$170k - $230k",
    type: "Full-time",
    posted: "1 day ago",
    matchScore: 85,
    matchReason: "Good fit, but requires more Go experience.",
    skills: ["Go", "Microservices", "Kafka"],
    missingSkills: ["Go", "gRPC"],
    logo: "bg-black"
  }
];

export default function JobsPage() {
  const [isCrawling, setIsCrawling] = useState(false);
  const [jobs, setJobs] = useState(MOCK_JOBS);
  const [searchQuery, setSearchQuery] = useState("");

  const handleCrawl = () => {
    setIsCrawling(true);
    // Simulate Crawling
    setTimeout(() => {
        setIsCrawling(false);
        // In real app, this would fetch new data
    }, 2500);
  };

  const filteredJobs = jobs.filter(job => 
    job.title.toLowerCase().includes(searchQuery.toLowerCase()) || 
    job.company.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="h-full p-8 max-w-7xl mx-auto space-y-8 animate-in fade-in duration-500">
        {/* Header Section */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div>
                <h2 className="text-3xl font-bold tracking-tight">Job Recommendations</h2>
                <p className="text-muted-foreground mt-1">
                    AI-curated opportunities based on your Profile Database.
                </p>
            </div>
            <div className="flex items-center gap-2">
                <Button variant="outline" onClick={handleCrawl} disabled={isCrawling}>
                    <RefreshCw className={cn("mr-2 h-4 w-4", isCrawling ? "animate-spin" : "")} />
                    {isCrawling ? "Scanning Web..." : "Scan for New Jobs"}
                </Button>
                <Button variant="outline" size="icon">
                    <Filter className="h-4 w-4" />
                </Button>
            </div>
        </div>

        {/* Search Bar */}
        <div className="relative">
            <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
            <Input 
                placeholder="Filter by title or company..." 
                className="pl-10 h-10"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
            />
        </div>

        {/* Job Grid */}
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {filteredJobs.map((job) => (
                <Card key={job.id} className="flex flex-col border-0 shadow-md bg-card hover:shadow-lg transition-all group">
                    <CardHeader>
                        <div className="flex justify-between items-start">
                            <div className="flex items-center gap-3">
                                <div className={`h-10 w-10 rounded-lg ${job.logo} flex items-center justify-center text-white font-bold`}>
                                    {job.company[0]}
                                </div>
                                <div>
                                    <CardTitle className="text-lg group-hover:text-primary transition-colors">{job.title}</CardTitle>
                                    <CardDescription>{job.company}</CardDescription>
                                </div>
                            </div>
                            <Badge className={cn(
                                "text-xs font-bold",
                                job.matchScore >= 90 ? "bg-green-500 hover:bg-green-600" : "bg-yellow-500 hover:bg-yellow-600"
                            )}>
                                {job.matchScore}% Match
                            </Badge>
                        </div>
                    </CardHeader>
                    <CardContent className="flex-1 space-y-4">
                        <div className="grid grid-cols-2 gap-2 text-sm text-muted-foreground">
                            <div className="flex items-center gap-1"><MapPin className="h-3 w-3" /> {job.location}</div>
                            <div className="flex items-center gap-1"><DollarSign className="h-3 w-3" /> {job.salary}</div>
                            <div className="flex items-center gap-1"><Briefcase className="h-3 w-3" /> {job.type}</div>
                            <div className="flex items-center gap-1"><Clock className="h-3 w-3" /> {job.posted}</div>
                        </div>

                        <div className="bg-muted/30 p-3 rounded-lg space-y-2">
                            <p className="text-xs font-semibold text-muted-foreground">Why you match:</p>
                            <p className="text-sm">{job.matchReason}</p>
                        </div>

                        <div className="space-y-2">
                             <div className="flex flex-wrap gap-2">
                                {job.skills.map(skill => (
                                    <Badge key={skill} variant="secondary" className="text-[10px] bg-muted/50">
                                        {skill}
                                    </Badge>
                                ))}
                             </div>
                             {job.missingSkills.length > 0 && (
                                <div className="flex items-center gap-2 text-xs text-red-500 mt-2">
                                    <XCircle className="h-3 w-3" />
                                    <span>Missing: {job.missingSkills.join(", ")}</span>
                                </div>
                             )}
                        </div>
                    </CardContent>
                    <CardFooter className="pt-4 border-t bg-muted/10">
                        <Button className="w-full gap-2">
                            Apply Now <ExternalLink className="h-4 w-4" />
                        </Button>
                    </CardFooter>
                </Card>
            ))}
        </div>
    </div>
  );
}
