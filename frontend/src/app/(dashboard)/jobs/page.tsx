"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Briefcase, MapPin, DollarSign, Clock, RefreshCw, Filter, ExternalLink, XCircle, Search, Loader2, AlertCircle, Bookmark, CheckCircle } from "lucide-react";
import { cn } from "@/lib/utils";
import { api, Job, JobMatch, JobMatchSummary, JobSalary } from "@/lib/api";
import { useAuth } from "@/lib/contexts/auth-context";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

// Helper function to format salary
const formatSalary = (salary: string | JobSalary | undefined): string => {
  if (!salary) return "";
  if (typeof salary === "string") return salary;
  if (salary.formatted) return salary.formatted;
  if (salary.min && salary.max) {
    const currency = salary.currency || "$";
    return `${currency}${salary.min.toLocaleString()} - ${currency}${salary.max.toLocaleString()}`;
  }
  if (salary.min) return `${salary.currency || "$"}${salary.min.toLocaleString()}+`;
  if (salary.max) return `Up to ${salary.currency || "$"}${salary.max.toLocaleString()}`;
  return "";
};

export default function JobsPage() {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const [isCrawling, setIsCrawling] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const [jobs, setJobs] = useState<Job[]>([]);
  const [matches, setMatches] = useState<JobMatch[]>([]);
  const [savedJobs, setSavedJobs] = useState<JobMatch[]>([]);
  const [appliedJobs, setAppliedJobs] = useState<JobMatch[]>([]);
  const [summary, setSummary] = useState<JobMatchSummary | null>(null);
  
  const [searchQuery, setSearchQuery] = useState("");
  const [activeTab, setActiveTab] = useState("recommended");

  // Fetch data on mount
  useEffect(() => {
    if (!authLoading && isAuthenticated) {
      fetchData();
    } else if (!authLoading && !isAuthenticated) {
      setIsLoading(false);
    }
  }, [authLoading, isAuthenticated]);

  const fetchData = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      // Fetch all data in parallel
      const [jobsRes, matchesRes, savedRes, appliedRes, summaryRes] = await Promise.all([
        api.getLatestJobs(),
        api.getJobMatches(),
        api.getSavedJobs(),
        api.getAppliedJobs(),
        api.getJobMatchSummary(),
      ]);

      if (jobsRes.success && jobsRes.data) {
        setJobs(jobsRes.data);
      }
      if (matchesRes.success && matchesRes.data) {
        setMatches(matchesRes.data.content || []);
      }
      if (savedRes.success && savedRes.data) {
        setSavedJobs(savedRes.data);
      }
      if (appliedRes.success && appliedRes.data) {
        setAppliedJobs(appliedRes.data);
      }
      if (summaryRes.success && summaryRes.data) {
        setSummary(summaryRes.data);
      }
    } catch (err) {
      console.error('Error fetching jobs:', err);
      setError('Failed to load jobs. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleGenerateMatches = async () => {
    setIsCrawling(true);
    try {
      const res = await api.generateJobMatches();
      if (res.success) {
        // Refresh data after generating new matches
        await fetchData();
      } else {
        setError(res.error || 'Failed to generate matches');
      }
    } catch (err) {
      setError('Failed to scan for new jobs');
    } finally {
      setIsCrawling(false);
    }
  };

  const handleSaveJob = async (matchId: string) => {
    try {
      const res = await api.saveJobMatch(matchId);
      if (res.success) {
        // Update local state
        setMatches(prev => prev.map(m => 
          m.id === matchId ? { ...m, status: 'SAVED' } : m
        ));
        await fetchData(); // Refresh to get updated lists
      }
    } catch (err) {
      console.error('Error saving job:', err);
    }
  };

  const handleApplyJob = async (matchId: string) => {
    try {
      const res = await api.applyToJob(matchId);
      if (res.success) {
        setMatches(prev => prev.map(m => 
          m.id === matchId ? { ...m, status: 'APPLIED' } : m
        ));
        await fetchData();
      }
    } catch (err) {
      console.error('Error applying to job:', err);
    }
  };

  // Filter jobs based on search
  const filterJobs = (jobList: JobMatch[] | undefined | null): JobMatch[] => {
    if (!jobList || !Array.isArray(jobList)) return [];
    if (!searchQuery) return jobList;
    const query = searchQuery.toLowerCase();
    return jobList.filter(match => 
      match.job?.title?.toLowerCase().includes(query) || 
      match.job?.company?.toLowerCase().includes(query) ||
      match.job?.location?.toLowerCase().includes(query)
    );
  };

  // Loading state
  if (authLoading || isLoading) {
    return (
      <div className="h-full flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  // Not authenticated
  if (!isAuthenticated) {
    return (
      <div className="h-full flex flex-col items-center justify-center gap-4">
        <AlertCircle className="h-12 w-12 text-muted-foreground" />
        <p className="text-muted-foreground">Please log in to view job recommendations</p>
      </div>
    );
  }

  const getLogoColor = (company: string) => {
    const colors = ['bg-indigo-500', 'bg-emerald-500', 'bg-rose-500', 'bg-amber-500', 'bg-violet-500', 'bg-cyan-500'];
    const index = company.length % colors.length;
    return colors[index];
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    
    if (diffHours < 1) return 'Just now';
    if (diffHours < 24) return `${diffHours} hours ago`;
    const diffDays = Math.floor(diffHours / 24);
    if (diffDays < 7) return `${diffDays} days ago`;
    return date.toLocaleDateString();
  };

  const JobCard = ({ match, showActions = true }: { match: JobMatch; showActions?: boolean }) => (
    <Card className="flex flex-col border-0 shadow-md bg-card hover:shadow-lg transition-all group">
      <CardHeader>
        <div className="flex justify-between items-start">
          <div className="flex items-center gap-3">
            <div className={`h-10 w-10 rounded-lg ${getLogoColor(match.job.company)} flex items-center justify-center text-white font-bold`}>
              {match.job.company[0]}
            </div>
            <div>
              <CardTitle className="text-lg group-hover:text-primary transition-colors">{match.job.title}</CardTitle>
              <CardDescription>{match.job.company}</CardDescription>
            </div>
          </div>
          <Badge className={cn(
            "text-xs font-bold",
            match.matchScore >= 90 ? "bg-green-500 hover:bg-green-600" : 
            match.matchScore >= 70 ? "bg-yellow-500 hover:bg-yellow-600" : 
            "bg-orange-500 hover:bg-orange-600"
          )}>
            {match.matchScore}% Match
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="flex-1 space-y-4">
        <div className="grid grid-cols-2 gap-2 text-sm text-muted-foreground">
          <div className="flex items-center gap-1">
            <MapPin className="h-3 w-3" /> {match.job.location}
          </div>
          {match.job.salary && formatSalary(match.job.salary) && (
            <div className="flex items-center gap-1">
              <DollarSign className="h-3 w-3" /> {formatSalary(match.job.salary)}
            </div>
          )}
          <div className="flex items-center gap-1">
            <Briefcase className="h-3 w-3" /> {match.job.isRemote ? 'Remote' : 'On-site'}
          </div>
          <div className="flex items-center gap-1">
            <Clock className="h-3 w-3" /> {formatDate(match.job.postedAt)}
          </div>
        </div>

        {match.matchReasons && match.matchReasons.length > 0 && (
          <div className="bg-muted/30 p-3 rounded-lg space-y-2">
            <p className="text-xs font-semibold text-muted-foreground">Why you match:</p>
            <p className="text-sm">{match.matchReasons.join('. ')}</p>
          </div>
        )}

        {match.job.requirements && match.job.requirements.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {match.job.requirements.slice(0, 4).map((req, i) => (
              <Badge key={i} variant="secondary" className="text-[10px] bg-muted/50">
                {req}
              </Badge>
            ))}
            {match.job.requirements.length > 4 && (
              <Badge variant="secondary" className="text-[10px] bg-muted/50">
                +{match.job.requirements.length - 4} more
              </Badge>
            )}
          </div>
        )}

        {/* Status Badge */}
        {match.status !== 'NEW' && (
          <div className="flex items-center gap-2 text-xs">
            {match.status === 'SAVED' && (
              <Badge variant="outline" className="text-blue-500 border-blue-500">
                <Bookmark className="h-3 w-3 mr-1" /> Saved
              </Badge>
            )}
            {match.status === 'APPLIED' && (
              <Badge variant="outline" className="text-green-500 border-green-500">
                <CheckCircle className="h-3 w-3 mr-1" /> Applied
              </Badge>
            )}
          </div>
        )}
      </CardContent>
      
      {showActions && match.status === 'NEW' && (
        <CardFooter className="pt-4 border-t bg-muted/10 flex gap-2">
          <Button 
            variant="outline" 
            size="sm" 
            className="flex-1"
            onClick={() => handleSaveJob(match.id)}
          >
            <Bookmark className="h-4 w-4 mr-1" /> Save
          </Button>
          <Button 
            size="sm" 
            className="flex-1 gap-2"
            onClick={() => handleApplyJob(match.id)}
          >
            Apply <ExternalLink className="h-4 w-4" />
          </Button>
        </CardFooter>
      )}
      
      {showActions && match.status === 'SAVED' && (
        <CardFooter className="pt-4 border-t bg-muted/10">
          <Button 
            className="w-full gap-2"
            onClick={() => handleApplyJob(match.id)}
          >
            Apply Now <ExternalLink className="h-4 w-4" />
          </Button>
        </CardFooter>
      )}

      {match.job.sourceUrl && (
        <CardFooter className={cn("border-t bg-muted/10", showActions && match.status !== 'APPLIED' ? "pt-2" : "pt-4")}>
          <a 
            href={match.job.sourceUrl} 
            target="_blank" 
            rel="noopener noreferrer"
            className="text-xs text-muted-foreground hover:text-primary flex items-center gap-1"
          >
            View Original <ExternalLink className="h-3 w-3" />
          </a>
        </CardFooter>
      )}
    </Card>
  );

  return (
    <div className="h-full overflow-auto p-8 max-w-7xl mx-auto space-y-8 animate-in fade-in duration-500">
      {/* Header Section */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Job Recommendations</h2>
          <p className="text-muted-foreground mt-1">
            AI-curated opportunities based on your Profile Database.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={handleGenerateMatches} disabled={isCrawling}>
            <RefreshCw className={cn("mr-2 h-4 w-4", isCrawling ? "animate-spin" : "")} />
            {isCrawling ? "Scanning..." : "Scan for New Jobs"}
          </Button>
          <Button variant="outline" size="icon">
            <Filter className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* Summary Stats */}
      {summary && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <Card className="border-0 shadow-sm">
            <CardContent className="p-4">
              <p className="text-2xl font-bold">{summary.totalMatches}</p>
              <p className="text-xs text-muted-foreground">Total Matches</p>
            </CardContent>
          </Card>
          <Card className="border-0 shadow-sm">
            <CardContent className="p-4">
              <p className="text-2xl font-bold text-green-500">{summary.newMatches}</p>
              <p className="text-xs text-muted-foreground">New Matches</p>
            </CardContent>
          </Card>
          <Card className="border-0 shadow-sm">
            <CardContent className="p-4">
              <p className="text-2xl font-bold text-blue-500">{summary.savedJobs}</p>
              <p className="text-xs text-muted-foreground">Saved Jobs</p>
            </CardContent>
          </Card>
          <Card className="border-0 shadow-sm">
            <CardContent className="p-4">
              <p className="text-2xl font-bold text-violet-500">{summary.appliedJobs}</p>
              <p className="text-xs text-muted-foreground">Applied</p>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div className="bg-destructive/10 text-destructive p-4 rounded-lg flex items-center gap-2">
          <AlertCircle className="h-4 w-4" />
          {error}
        </div>
      )}

      {/* Search Bar */}
      <div className="relative">
        <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
        <Input 
          placeholder="Filter by title, company, or location..." 
          className="pl-10 h-10"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
      </div>

      {/* Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="recommended">
            Recommended ({matches.filter(m => m.status === 'NEW').length})
          </TabsTrigger>
          <TabsTrigger value="saved">
            Saved ({savedJobs.length})
          </TabsTrigger>
          <TabsTrigger value="applied">
            Applied ({appliedJobs.length})
          </TabsTrigger>
        </TabsList>

        <TabsContent value="recommended" className="mt-6">
          {filterJobs(matches).length === 0 ? (
            <div className="text-center py-12">
              <Briefcase className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
              <p className="text-muted-foreground">No job recommendations yet.</p>
              <p className="text-sm text-muted-foreground mt-1">
                Upload your resume and click "Scan for New Jobs" to get started.
              </p>
            </div>
          ) : (
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
              {filterJobs(matches).map((match) => (
                <JobCard key={match.id} match={match} />
              ))}
            </div>
          )}
        </TabsContent>

        <TabsContent value="saved" className="mt-6">
          {savedJobs.length === 0 ? (
            <div className="text-center py-12">
              <Bookmark className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
              <p className="text-muted-foreground">No saved jobs yet.</p>
            </div>
          ) : (
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
              {filterJobs(savedJobs).map((match) => (
                <JobCard key={match.id} match={match} />
              ))}
            </div>
          )}
        </TabsContent>

        <TabsContent value="applied" className="mt-6">
          {appliedJobs.length === 0 ? (
            <div className="text-center py-12">
              <CheckCircle className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
              <p className="text-muted-foreground">No applications yet.</p>
            </div>
          ) : (
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
              {filterJobs(appliedJobs).map((match) => (
                <JobCard key={match.id} match={match} showActions={false} />
              ))}
            </div>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}
