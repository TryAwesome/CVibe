"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Loader2, CheckCircle2, Circle, ArrowRight, BookOpen, Code, Upload, AlertCircle, Target, TrendingUp, Trash2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { api, GrowthGoal, GapAnalysis, LearningPath, GrowthSummary, SkillGap, Milestone } from "@/lib/api";
import { useAuth } from "@/lib/contexts/auth-context";

export default function GrowthPage() {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  
  const [isLoading, setIsLoading] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // Data state
  const [goals, setGoals] = useState<GrowthGoal[]>([]);
  const [summary, setSummary] = useState<GrowthSummary | null>(null);
  const [selectedGoal, setSelectedGoal] = useState<GrowthGoal | null>(null);
  const [gapAnalysis, setGapAnalysis] = useState<GapAnalysis | null>(null);
  const [learningPaths, setLearningPaths] = useState<LearningPath[]>([]);
  
  // Form state
  const [company, setCompany] = useState("");
  const [position, setPosition] = useState("");

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
      const [goalsRes, summaryRes] = await Promise.all([
        api.getGoals(),
        api.getGrowthSummary(),
      ]);

      if (goalsRes.success && goalsRes.data) {
        setGoals(goalsRes.data);
        // Auto-select primary goal or first goal
        const primaryGoal = goalsRes.data.find(g => g.isPrimary) || goalsRes.data[0];
        if (primaryGoal) {
          await selectGoal(primaryGoal);
        }
      }
      if (summaryRes.success && summaryRes.data) {
        setSummary(summaryRes.data);
      }
    } catch (err) {
      console.error('Error fetching growth data:', err);
      setError('Failed to load growth data');
    } finally {
      setIsLoading(false);
    }
  };

  const selectGoal = async (goal: GrowthGoal) => {
    setSelectedGoal(goal);
    try {
      const [gapsRes, pathsRes] = await Promise.all([
        api.getGaps(goal.id),
        api.getLearningPaths(goal.id),
      ]);

      if (gapsRes.success && gapsRes.data) {
        setGapAnalysis({
          score: goal.progress,
          gaps: gapsRes.data,
          strengths: [],
        });
      }
      if (pathsRes.success && pathsRes.data) {
        setLearningPaths(pathsRes.data);
      }
    } catch (err) {
      console.error('Error fetching goal details:', err);
    }
  };

  const handleCreateGoal = async () => {
    if (!company || !position) return;
    setAnalyzing(true);
    setError(null);

    try {
      // Create the goal
      const goalRes = await api.createGoal({
        targetCompany: company,
        targetPosition: position,
      });

      if (goalRes.success && goalRes.data) {
        const newGoal = goalRes.data;
        setGoals(prev => [...prev, newGoal]);
        setSelectedGoal(newGoal);

        // Trigger analysis
        const analysisRes = await api.analyzeGoal(newGoal.id);
        if (analysisRes.success && analysisRes.data) {
          setGapAnalysis(analysisRes.data);
        }

        // Generate learning paths
        const pathsRes = await api.generateLearningPaths(newGoal.id);
        if (pathsRes.success && pathsRes.data) {
          setLearningPaths(pathsRes.data);
        }

        // Clear form
        setCompany("");
        setPosition("");
        
        // Refresh summary
        const summaryRes = await api.getGrowthSummary();
        if (summaryRes.success && summaryRes.data) {
          setSummary(summaryRes.data);
        }
      } else {
        setError(goalRes.error || 'Failed to create goal');
      }
    } catch (err) {
      console.error('Error creating goal:', err);
      setError('Failed to create goal');
    } finally {
      setAnalyzing(false);
    }
  };

  const handleDeleteGoal = async (goalId: string) => {
    try {
      const res = await api.deleteGoal(goalId);
      if (res.success) {
        setGoals(prev => prev.filter(g => g.id !== goalId));
        if (selectedGoal?.id === goalId) {
          setSelectedGoal(null);
          setGapAnalysis(null);
          setLearningPaths([]);
        }
      }
    } catch (err) {
      console.error('Error deleting goal:', err);
    }
  };

  const handleCompleteMilestone = async (milestoneId: string) => {
    try {
      const res = await api.completeMilestone(milestoneId);
      if (res.success) {
        // Update local state
        setLearningPaths(prev => prev.map(path => ({
          ...path,
          milestones: path.milestones.map(m => 
            m.id === milestoneId ? { ...m, isCompleted: true } : m
          )
        })));
        // Refresh summary
        const summaryRes = await api.getGrowthSummary();
        if (summaryRes.success && summaryRes.data) {
          setSummary(summaryRes.data);
        }
      }
    } catch (err) {
      console.error('Error completing milestone:', err);
    }
  };

  const handleUncompleteMilestone = async (milestoneId: string) => {
    try {
      const res = await api.uncompleteMilestone(milestoneId);
      if (res.success) {
        setLearningPaths(prev => prev.map(path => ({
          ...path,
          milestones: path.milestones.map(m => 
            m.id === milestoneId ? { ...m, isCompleted: false } : m
          )
        })));
      }
    } catch (err) {
      console.error('Error uncompleting milestone:', err);
    }
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
        <p className="text-muted-foreground">Please log in to access Career Growth</p>
      </div>
    );
  }

  return (
    <div className="h-full p-8 max-w-6xl mx-auto space-y-8 animate-in fade-in duration-500 overflow-y-auto">
      <div className="flex flex-col gap-2">
        <h2 className="text-3xl font-bold tracking-tight">Career Growth & Learning Path</h2>
        <p className="text-muted-foreground">
          Bridge the gap between your current profile and your dream job with AI-curated plans.
        </p>
      </div>

      {/* Summary Stats */}
      {summary && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <Card className="border-0 shadow-sm">
            <CardContent className="p-4">
              <Target className="h-5 w-5 text-primary mb-2" />
              <p className="text-2xl font-bold">{summary.activeGoals}</p>
              <p className="text-xs text-muted-foreground">Active Goals</p>
            </CardContent>
          </Card>
          <Card className="border-0 shadow-sm">
            <CardContent className="p-4">
              <CheckCircle2 className="h-5 w-5 text-green-500 mb-2" />
              <p className="text-2xl font-bold">{summary.completedMilestones}</p>
              <p className="text-xs text-muted-foreground">Milestones Done</p>
            </CardContent>
          </Card>
          <Card className="border-0 shadow-sm">
            <CardContent className="p-4">
              <Circle className="h-5 w-5 text-blue-500 mb-2" />
              <p className="text-2xl font-bold">{summary.totalMilestones}</p>
              <p className="text-xs text-muted-foreground">Total Milestones</p>
            </CardContent>
          </Card>
          <Card className="border-0 shadow-sm">
            <CardContent className="p-4">
              <TrendingUp className="h-5 w-5 text-violet-500 mb-2" />
              <p className="text-2xl font-bold">{summary.overallProgress}%</p>
              <p className="text-xs text-muted-foreground">Overall Progress</p>
            </CardContent>
          </Card>
        </div>
      )}

      {error && (
        <div className="bg-destructive/10 text-destructive p-4 rounded-lg flex items-center gap-2">
          <AlertCircle className="h-4 w-4" />
          {error}
        </div>
      )}

      {/* Existing Goals */}
      {goals.length > 0 && (
        <Card className="border-0 shadow-md bg-card/50">
          <CardHeader>
            <CardTitle>Your Goals</CardTitle>
            <CardDescription>Select a goal to view your learning path</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-wrap gap-3">
            {goals.map(goal => (
              <div 
                key={goal.id}
                className={cn(
                  "flex items-center gap-2 px-4 py-2 rounded-lg cursor-pointer transition-all",
                  selectedGoal?.id === goal.id 
                    ? "bg-primary text-primary-foreground" 
                    : "bg-muted hover:bg-muted/80"
                )}
                onClick={() => selectGoal(goal)}
              >
                <Target className="h-4 w-4" />
                <span className="font-medium">{goal.targetCompany}</span>
                <span className="text-sm opacity-80">• {goal.targetPosition}</span>
                {goal.isPrimary && <Badge variant="secondary" className="text-[10px]">Primary</Badge>}
                <button
                  className="ml-2 hover:text-destructive"
                  onClick={(e) => { e.stopPropagation(); handleDeleteGoal(goal.id); }}
                >
                  <Trash2 className="h-3 w-3" />
                </button>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {/* Create New Goal */}
      <Card className="border-0 shadow-lg bg-card/50 backdrop-blur-sm">
        <CardHeader>
          <CardTitle>Set a New Goal</CardTitle>
          <CardDescription>Where do you want to be in 3 months?</CardDescription>
        </CardHeader>
        <CardContent>
          <Tabs defaultValue="manual" className="w-full">
            <TabsList className="grid w-full grid-cols-2 mb-4">
              <TabsTrigger value="manual">Manual Input</TabsTrigger>
              <TabsTrigger value="image">Upload Job Description</TabsTrigger>
            </TabsList>
            
            <TabsContent value="manual" className="space-y-4">
              <div className="grid gap-6 md:grid-cols-2">
                            <div className="space-y-2">
                                <Label>Target Company</Label>
                                <Input 
                                    placeholder="e.g. Netflix" 
                                    value={company}
                                    onChange={(e) => setCompany(e.target.value)}
                                />
                            </div>
                            <div className="space-y-2">
                                <Label>Target Position</Label>
                                <Input 
                                    placeholder="e.g. Senior Backend Engineer" 
                                    value={position}
                                    onChange={(e) => setPosition(e.target.value)}
                                />
                            </div>
                        </div>
                        <Button 
                            onClick={handleCreateGoal} 
                            disabled={analyzing || !company || !position}
                            className="w-full mt-4"
                        >
                            {analyzing ? (
                                <>
                                    <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Analyzing...
                                </>
                            ) : (
                                "Create Goal & Analyze"
                            )}
                        </Button>
                    </TabsContent>

                    <TabsContent value="image">
                        <div className="border-2 border-dashed rounded-xl p-10 flex flex-col items-center justify-center text-center hover:bg-accent/50 transition-colors cursor-pointer group">
                            <div className="bg-primary/10 p-4 rounded-full mb-4 group-hover:scale-110 transition-transform">
                                <Upload className="h-8 w-8 text-primary" />
                            </div>
                            <h3 className="text-lg font-semibold">Upload Job Screenshot</h3>
                            <p className="text-sm text-muted-foreground mt-2 max-w-xs">
                                Drag and drop a screenshot of the job description here, or click to browse.
                            </p>
                            <Input type="file" className="hidden" />
                            <Button variant="outline" className="mt-6">
                                Select Image File
                            </Button>
                        </div>
                    </TabsContent>
                </Tabs>
            </CardContent>
        </Card>

        {/* Results Section */}
        {selectedGoal && gapAnalysis && (
            <div className="space-y-8 animate-in slide-in-from-bottom-8 duration-700">
                {/* Score & Gaps Overview */}
                <div className="grid gap-6 md:grid-cols-3">
                    <Card className="md:col-span-1 border-primary/20 bg-primary/5">
                        <CardHeader>
                            <CardTitle>Match Score</CardTitle>
                            <CardDescription>For {selectedGoal.targetCompany} - {selectedGoal.targetPosition}</CardDescription>
                        </CardHeader>
                        <CardContent className="flex items-center justify-center py-6">
                            <div className="relative flex items-center justify-center h-32 w-32 rounded-full border-8 border-primary/20">
                                <span className="text-4xl font-bold text-primary">{gapAnalysis.score}%</span>
                            </div>
                        </CardContent>
                    </Card>

                    <Card className="md:col-span-2">
                        <CardHeader>
                            <CardTitle>Identified Gaps</CardTitle>
                            <CardDescription>Key areas to improve</CardDescription>
                        </CardHeader>
                        <CardContent>
                            <div className="flex flex-wrap gap-3">
                                {gapAnalysis.gaps.map((gap) => (
                                    <Badge key={gap.id} variant="destructive" className="text-sm px-3 py-1">
                                        {gap.skill} (Level {gap.currentLevel} → {gap.requiredLevel})
                                    </Badge>
                                ))}
                                {gapAnalysis.gaps.length === 0 && (
                                    <p className="text-muted-foreground">No gaps identified yet. Generate a learning path to analyze.</p>
                                )}
                            </div>
                            {gapAnalysis.strengths.length > 0 && (
                                <div className="mt-4">
                                    <p className="text-xs font-semibold text-muted-foreground mb-2">Your Strengths</p>
                                    <div className="flex flex-wrap gap-2">
                                        {gapAnalysis.strengths.map((s, i) => (
                                            <Badge key={i} variant="outline" className="bg-green-500/10 text-green-600">
                                                {s}
                                            </Badge>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </CardContent>
                    </Card>
                </div>

                {/* Learning Paths */}
                {learningPaths.length > 0 && (
                    <div className="space-y-4">
                        <h3 className="text-xl font-semibold flex items-center gap-2">
                            <ArrowRight className="h-5 w-5 text-primary" /> Your Personalized Learning Path
                        </h3>
                        {learningPaths.map(path => (
                            <Card key={path.id} className="mb-4">
                                <CardHeader>
                                    <CardTitle>{path.title}</CardTitle>
                                    <CardDescription>{path.description} • {path.duration}</CardDescription>
                                </CardHeader>
                                <CardContent>
                                    <div className="relative border-l-2 border-muted ml-4 space-y-6 pl-8 py-2">
                                        {path.milestones.sort((a, b) => a.order - b.order).map((milestone, index) => (
                                            <div key={milestone.id} className="relative">
                                                {/* Timeline Dot */}
                                                <span 
                                                    className={cn(
                                                        "absolute -left-[41px] top-1 flex h-6 w-6 items-center justify-center rounded-full border-2 ring-4 ring-background cursor-pointer transition-colors",
                                                        milestone.isCompleted 
                                                            ? "bg-green-500 border-green-500 text-white" 
                                                            : "bg-background border-primary"
                                                    )}
                                                    onClick={() => milestone.isCompleted 
                                                        ? handleUncompleteMilestone(milestone.id)
                                                        : handleCompleteMilestone(milestone.id)
                                                    }
                                                >
                                                    {milestone.isCompleted ? (
                                                        <CheckCircle2 className="h-3 w-3" />
                                                    ) : (
                                                        <span className="text-[10px] font-bold">{index + 1}</span>
                                                    )}
                                                </span>
                                                
                                                <div className={cn(
                                                    "p-4 rounded-lg border transition-all",
                                                    milestone.isCompleted 
                                                        ? "bg-green-500/5 border-green-500/20" 
                                                        : "hover:border-primary/50"
                                                )}>
                                                    <div className="flex justify-between items-start">
                                                        <div>
                                                            <h4 className={cn(
                                                                "font-semibold",
                                                                milestone.isCompleted && "line-through opacity-70"
                                                            )}>{milestone.title}</h4>
                                                            <p className="text-sm text-muted-foreground mt-1">{milestone.description}</p>
                                                        </div>
                                                        {milestone.type === 'PROJECT' ? (
                                                            <div className="bg-blue-500/10 p-2 rounded-lg text-blue-500">
                                                                <Code className="h-4 w-4" />
                                                            </div>
                                                        ) : (
                                                            <div className="bg-green-500/10 p-2 rounded-lg text-green-500">
                                                                <BookOpen className="h-4 w-4" />
                                                            </div>
                                                        )}
                                                    </div>
                                                    {milestone.resources.length > 0 && (
                                                        <div className="mt-3 flex flex-wrap gap-2">
                                                            {milestone.resources.map((res, i) => (
                                                                <Badge key={i} variant="outline" className="bg-background text-xs">
                                                                    {res}
                                                                </Badge>
                                                            ))}
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </CardContent>
                            </Card>
                        ))}
                    </div>
                )}

                {/* Generate Paths Button if no paths */}
                {learningPaths.length === 0 && selectedGoal && (
                    <div className="text-center py-8">
                        <p className="text-muted-foreground mb-4">No learning paths generated yet for this goal.</p>
                        <Button onClick={async () => {
                            setAnalyzing(true);
                            try {
                                const res = await api.generateLearningPaths(selectedGoal.id);
                                if (res.success && res.data) {
                                    setLearningPaths(res.data);
                                }
                            } catch (err) {
                                console.error('Error generating paths:', err);
                            } finally {
                                setAnalyzing(false);
                            }
                        }} disabled={analyzing}>
                            {analyzing ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                            Generate Learning Path
                        </Button>
                    </div>
                )}
            </div>
        )}

        {/* Empty State */}
        {goals.length === 0 && !analyzing && (
            <div className="text-center py-12">
                <Target className="h-16 w-16 mx-auto text-muted-foreground mb-4" />
                <h3 className="text-xl font-semibold mb-2">No Goals Yet</h3>
                <p className="text-muted-foreground">Set your first career goal above to get started with personalized learning paths.</p>
            </div>
        )}
    </div>
  );
}
