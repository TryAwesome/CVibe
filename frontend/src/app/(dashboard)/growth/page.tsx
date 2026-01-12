"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Loader2, CheckCircle2, Circle, ArrowRight, BookOpen, Code, Upload } from "lucide-react";
import { cn } from "@/lib/utils";

type Step = {
  id: number;
  title: string;
  description: string;
  duration: string;
  resources: string[];
  type: "learn" | "project";
};

type AnalysisResult = {
  score: number;
  gaps: string[];
  roadmap: Step[];
};

export default function GrowthPage() {
  const [analyzing, setAnalyzing] = useState(false);
  const [result, setResult] = useState<AnalysisResult | null>(null);
  
  const [company, setCompany] = useState("");
  const [position, setPosition] = useState("");

  const handleAnalyze = () => {
    if (!company || !position) return;
    setAnalyzing(true);
    setResult(null);

    // Mock Analysis Delay
    setTimeout(() => {
        setResult({
            score: 65,
            gaps: ["System Design", "Kubernetes", "Go Concurrency"],
            roadmap: [
                {
                    id: 1,
                    title: "Master Go Fundamentals",
                    description: "Focus on Goroutines and Channels for high-performance concurrency.",
                    duration: "1 Week",
                    resources: ["Go by Example", "Effective Go"],
                    type: "learn"
                },
                {
                    id: 2,
                    title: "Container Orchestration with Kubernetes",
                    description: "Learn how to deploy and manage scaled applications.",
                    duration: "2 Weeks",
                    resources: ["K8s Official Docs", "MiniKube Tutorial"],
                    type: "learn"
                },
                {
                    id: 3,
                    title: "Build a Distributed Key-Value Store",
                    description: "Apply your Go and System Design knowledge to build a real project.",
                    duration: "3 Weeks",
                    resources: ["MIT 6.824 Labs"],
                    type: "project"
                }
            ]
        });
        setAnalyzing(false);
    }, 2000);
  };

  return (
    <div className="h-full p-8 max-w-6xl mx-auto space-y-8 animate-in fade-in duration-500">
        <div className="flex flex-col gap-2">
            <h2 className="text-3xl font-bold tracking-tight">Career Growth & Learning Path</h2>
            <p className="text-muted-foreground">
                Bridge the gap between your current profile and your dream job with AI-curated plans.
            </p>
        </div>

        {/* Input Section */}
        <Card className="border-0 shadow-lg bg-card/50 backdrop-blur-sm">
            <CardHeader>
                <CardTitle>Set Your Goal</CardTitle>
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
                            onClick={handleAnalyze} 
                            disabled={analyzing || !company || !position}
                            className="w-full mt-4"
                        >
                            {analyzing ? (
                                <>
                                    <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Analyzing...
                                </>
                            ) : (
                                "Analyze Gap & Generate Plan"
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
                            <Button variant="outline" className="mt-6" onClick={() => {
                                // Mock upload behavior
                                setCompany("Detected: TechCorp");
                                setPosition("Detected: AI Engineer");
                                handleAnalyze();
                            }}>
                                Select Image File
                            </Button>
                        </div>
                    </TabsContent>
                </Tabs>
            </CardContent>
        </Card>

        {/* Results Section */}
        {result && (
            <div className="space-y-8 animate-in slide-in-from-bottom-8 duration-700">
                {/* Score & Gaps Overview */}
                <div className="grid gap-6 md:grid-cols-3">
                    <Card className="md:col-span-1 border-primary/20 bg-primary/5">
                        <CardHeader>
                            <CardTitle>Match Score</CardTitle>
                            <CardDescription>Based on your current resume</CardDescription>
                        </CardHeader>
                        <CardContent className="flex items-center justify-center py-6">
                            <div className="relative flex items-center justify-center h-32 w-32 rounded-full border-8 border-primary/20">
                                <span className="text-4xl font-bold text-primary">{result.score}%</span>
                            </div>
                        </CardContent>
                    </Card>

                    <Card className="md:col-span-2">
                        <CardHeader>
                            <CardTitle>Identified Gaps</CardTitle>
                            <CardDescription>Key areas missing from your profile</CardDescription>
                        </CardHeader>
                        <CardContent>
                            <div className="flex flex-wrap gap-3">
                                {result.gaps.map((gap) => (
                                    <Badge key={gap} variant="destructive" className="text-sm px-3 py-1">
                                        {gap}
                                    </Badge>
                                ))}
                            </div>
                            <p className="mt-4 text-sm text-muted-foreground">
                                We have curated a personalized learning path below to address these gaps efficiently.
                            </p>
                        </CardContent>
                    </Card>
                </div>

                {/* Roadmap Timeline */}
                <div className="space-y-4">
                    <h3 className="text-xl font-semibold flex items-center gap-2">
                        <ArrowRight className="h-5 w-5 text-primary" /> Your Personalized Learning Path
                    </h3>
                    <div className="relative border-l-2 border-muted ml-4 space-y-8 pl-8 py-2">
                        {result.roadmap.map((step, index) => (
                            <div key={step.id} className="relative">
                                {/* Timeline Dot */}
                                <span className="absolute -left-[41px] top-1 flex h-6 w-6 items-center justify-center rounded-full bg-background border-2 border-primary ring-4 ring-background">
                                    <span className="text-[10px] font-bold">{index + 1}</span>
                                </span>
                                
                                <Card className="hover:border-primary/50 transition-colors">
                                    <CardHeader>
                                        <div className="flex justify-between items-start">
                                            <div>
                                                <CardTitle className="text-lg">{step.title}</CardTitle>
                                                <CardDescription>{step.duration} • {step.type === 'project' ? 'Hands-on Project' : 'Concept Mastery'}</CardDescription>
                                            </div>
                                            {step.type === 'project' ? (
                                                <div className="bg-blue-500/10 p-2 rounded-lg text-blue-500">
                                                    <Code className="h-5 w-5" />
                                                </div>
                                            ) : (
                                                <div className="bg-green-500/10 p-2 rounded-lg text-green-500">
                                                    <BookOpen className="h-5 w-5" />
                                                </div>
                                            )}
                                        </div>
                                    </CardHeader>
                                    <CardContent className="space-y-4">
                                        <p className="text-sm">{step.description}</p>
                                        <div className="bg-muted/30 p-3 rounded-md">
                                            <p className="text-xs font-semibold mb-2 text-muted-foreground uppercase">Recommended Resources</p>
                                            <div className="flex flex-wrap gap-2">
                                                {step.resources.map(res => (
                                                    <Badge key={res} variant="outline" className="bg-background hover:bg-accent cursor-pointer">
                                                        {res}
                                                    </Badge>
                                                ))}
                                            </div>
                                        </div>
                                    </CardContent>
                                </Card>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        )}
    </div>
  );
}
