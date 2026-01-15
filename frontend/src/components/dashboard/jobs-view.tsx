"use client";

import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Briefcase, Building2, MapPin, ArrowUpRight, Loader2, Bookmark, BookmarkCheck } from "lucide-react";
import api, { JobMatch } from "@/lib/api";

export function JobsView() {
  const [jobs, setJobs] = useState<JobMatch[]>([]);
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState<string | null>(null);

  useEffect(() => {
    loadJobs();
  }, []);

  const loadJobs = async () => {
    try {
      setLoading(true);
      const res = await api.getJobMatches();
      if (res.success && res.data) {
        setJobs(res.data.content || []);
      }
    } catch (error) {
      console.error("Failed to load jobs:", error);
    } finally {
      setLoading(false);
    }
  };

  const toggleSave = async (jobId: string, currentlySaved: boolean) => {
    try {
      setSavingId(jobId);
      if (currentlySaved) {
        // Would need unsave endpoint
      } else {
        await api.saveJobMatch(jobId);
      }
      setJobs(prev => prev.map(j => 
        j.id === jobId ? { ...j, isSaved: !currentlySaved } : j
      ));
    } catch (error) {
      console.error("Failed to toggle save:", error);
    } finally {
      setSavingId(null);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (jobs.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-64 text-muted-foreground">
        <Briefcase className="h-12 w-12 mb-4 opacity-50" />
        <p>No matching jobs found</p>
        <p className="text-sm">Upload a resume to get personalized job recommendations</p>
      </div>
    );
  }

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3 animate-in fade-in slide-in-from-bottom-4 duration-500">
      {jobs.map((job) => (
        <Card key={job.id} className="group hover:border-primary/50 transition-colors">
          <CardHeader>
            <div className="flex justify-between items-start">
              <div className="space-y-1">
                <CardTitle className="text-lg group-hover:text-primary transition-colors">
                  {job.job?.title || "Unknown Position"}
                </CardTitle>
                <CardDescription className="flex items-center gap-1">
                  <Building2 className="h-3 w-3" /> {job.job?.company || "Unknown Company"}
                </CardDescription>
              </div>
              <div className="flex items-center gap-2">
                <Badge variant={job.matchScore && job.matchScore > 90 ? "default" : "secondary"} className="text-xs">
                  {job.matchScore || 0}% Match
                </Badge>
                <Button
                  size="icon"
                  variant="ghost"
                  className="h-8 w-8"
                  disabled={savingId === job.id}
                  onClick={() => toggleSave(job.id, job.isSaved || false)}
                >
                  {savingId === job.id ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : job.isSaved ? (
                    <BookmarkCheck className="h-4 w-4 text-primary" />
                  ) : (
                    <Bookmark className="h-4 w-4" />
                  )}
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <MapPin className="h-3 w-3" /> {job.job?.location || "Remote"}
                {job.job?.createdAt && (
                  <>
                    <span className="mx-1">•</span>
                    <span>{new Date(job.job.createdAt).toLocaleDateString()}</span>
                  </>
                )}
              </div>
              {job.job?.skills && job.job.skills.length > 0 && (
                <div className="flex flex-wrap gap-2">
                  {job.job.skills.slice(0, 4).map(tag => (
                    <Badge key={tag} variant="outline" className="text-[10px] px-2 py-0 h-5">
                      {tag}
                    </Badge>
                  ))}
                </div>
              )}
              <Button 
                variant="ghost" 
                className="w-full text-xs group-hover:bg-primary/10"
                onClick={() => window.open(job.job?.url || "#", "_blank")}
              >
                View Details <ArrowUpRight className="h-3 w-3 ml-2" />
              </Button>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
