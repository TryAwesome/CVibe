"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { FileText, Download, Save, RefreshCw, Upload, Code, Loader2, Sparkles, CheckCircle2 } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";

// Mock Templates
const TEMPLATES = [
  { id: "modern", name: "Modern Tech", description: "Clean sans-serif style perfect for SW Engineers.", previewColor: "bg-blue-100" },
  { id: "classic", name: "Ivy League Classic", description: "Traditional serif font, highly professional.", previewColor: "bg-slate-100" },
  { id: "minimal", name: "Minimalist", description: "Focus purely on content with ample whitespace.", previewColor: "bg-white border" },
];

const INITIAL_LATEX = `\\documentclass{article}
\\usepackage{titlesec}
\\usepackage{enumitem}

\\begin{document}

\\section*{Haosen Shi}
\\textit{Software Engineer} \\ 
Email: demo@cvibe.ai \\ 
Phone: (555) 123-4567

\\section*{Education}
\\textbf{Stanford University} \\hfill 2022 - 2024 \\ 
Master of Computer Science

\\section*{Experience}
\\textbf{Google} -- Software Engineering Intern \\hfill Summer 2023 \\ 
- Optimized distributed storage system latency by 20%. \\ 
- Implemented new React components for internal dashboard.

\\section*{Skills}
Java, Python, TypeScript, React, Next.js, Kubernetes

\\end{document}
`;

export default function ResumeBuilderPage() {
  // State for Wizard
  const [step, setStep] = useState<"setup" | "generating" | "editor">("setup");
  
  // Setup State
  const [selectedTemplate, setSelectedTemplate] = useState<string | null>(null);
  const [jdFile, setJdFile] = useState<File | null>(null);
  
  // Editor State
  const [latexCode, setLatexCode] = useState(INITIAL_LATEX);
  const [isCompiling, setIsCompiling] = useState(false);

  // Generation Animation State
  const [generationStatus, setGenerationStatus] = useState("Initializing...");

  const handleGenerate = () => {
    if (!selectedTemplate) return;
    setStep("generating");

    // Mock Generation Steps
    const steps = [
        "Scanning Job Description...",
        "Extracting keywords: React, System Design, Go...",
        "Retrieving matching data from Profile Database...",
        "Optimizing bullet points for ATS...",
        "Generating LaTeX code..."
    ];

    let i = 0;
    const interval = setInterval(() => {
        if (i < steps.length) {
            setGenerationStatus(steps[i]);
            i++;
        } else {
            clearInterval(interval);
            setStep("editor");
        }
    }, 800);
  };

  const handleCompile = () => {
    setIsCompiling(true);
    setTimeout(() => setIsCompiling(false), 1000); // Mock delay
  };

  // 1. SETUP VIEW (Select Template & Upload JD)
  if (step === "setup") {
    return (
        <div className="h-full p-8 animate-in fade-in duration-500 overflow-y-auto">
            <div className="max-w-6xl mx-auto space-y-8">
                <div className="text-center space-y-2">
                    <h2 className="text-3xl font-bold tracking-tight">AI Resume Builder</h2>
                    <p className="text-muted-foreground max-w-2xl mx-auto">
                        We'll tailor your resume specifically for the job. Select a template and upload the job description, and our AI will pull the most relevant experience from your Profile Database.
                    </p>
                </div>

                <div className="grid gap-8 lg:grid-cols-2">
                    {/* Left: Template Selection */}
                    <Card className="border-0 shadow-md bg-card/50">
                        <CardHeader>
                            <CardTitle className="flex items-center gap-2">
                                <span className="bg-primary/10 text-primary w-6 h-6 rounded-full flex items-center justify-center text-xs">1</span>
                                Choose Template
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="grid gap-4 sm:grid-cols-2">
                            {TEMPLATES.map((tpl) => (
                                <div 
                                    key={tpl.id} 
                                    className={cn(
                                        "cursor-pointer border-2 rounded-xl p-4 transition-all hover:scale-[1.02]",
                                        selectedTemplate === tpl.id ? "border-primary bg-primary/5" : "border-transparent bg-muted/50 hover:bg-muted"
                                    )}
                                    onClick={() => setSelectedTemplate(tpl.id)}
                                >
                                    <div className={`h-24 w-full ${tpl.previewColor} mb-3 rounded-md flex items-center justify-center`}>
                                        <FileText className="h-8 w-8 text-muted-foreground/30" />
                                    </div>
                                    <h4 className="font-semibold text-sm">{tpl.name}</h4>
                                    <p className="text-xs text-muted-foreground mt-1 line-clamp-2">{tpl.description}</p>
                                    {selectedTemplate === tpl.id && (
                                        <div className="mt-2 flex items-center text-xs text-primary font-medium">
                                            <CheckCircle2 className="h-3 w-3 mr-1" /> Selected
                                        </div>
                                    )}
                                </div>
                            ))}
                        </CardContent>
                    </Card>

                    {/* Right: JD Upload */}
                    <Card className="border-0 shadow-md bg-card/50 flex flex-col">
                        <CardHeader>
                            <CardTitle className="flex items-center gap-2">
                                <span className="bg-primary/10 text-primary w-6 h-6 rounded-full flex items-center justify-center text-xs">2</span>
                                Upload Job Description
                            </CardTitle>
                            <CardDescription>Upload a screenshot or PDF of the job post.</CardDescription>
                        </CardHeader>
                        <CardContent className="flex-1 flex flex-col justify-center">
                            <div 
                                className={cn(
                                    "border-2 border-dashed rounded-xl p-10 flex flex-col items-center justify-center text-center transition-colors h-64",
                                    jdFile ? "border-primary bg-primary/5" : "hover:bg-accent/50 cursor-pointer"
                                )}
                                onClick={() => !jdFile && document.getElementById('jd-upload')?.click()}
                            >
                                {jdFile ? (
                                    <>
                                        <FileText className="h-12 w-12 text-primary mb-4" />
                                        <p className="font-medium text-lg">{jdFile.name}</p>
                                        <p className="text-sm text-muted-foreground">Ready to analyze</p>
                                        <Button variant="ghost" size="sm" className="mt-4 text-destructive hover:text-destructive" onClick={(e) => { e.stopPropagation(); setJdFile(null); }}>
                                            Remove
                                        </Button>
                                    </>
                                ) : (
                                    <>
                                        <div className="bg-muted p-4 rounded-full mb-4">
                                            <Upload className="h-8 w-8 text-muted-foreground" />
                                        </div>
                                        <h3 className="text-lg font-semibold">Drop Screenshot Here</h3>
                                        <p className="text-sm text-muted-foreground mt-2 max-w-xs">
                                            Or click to browse files
                                        </p>
                                        <Input 
                                            id="jd-upload" 
                                            type="file" 
                                            className="hidden" 
                                            onChange={(e) => e.target.files && setJdFile(e.target.files[0])} 
                                        />
                                    </>
                                )}
                            </div>
                        </CardContent>
                    </Card>
                </div>

                <div className="flex justify-center pt-8">
                    <Button 
                        size="lg" 
                        className="w-full max-w-md text-lg h-14 shadow-xl shadow-primary/20"
                        disabled={!selectedTemplate} // JD is optional but recommended? Let's assume JD is optional for generic resume, but prompt implied it's key. Let's make it optional for UX flow but highly encouraged.
                        onClick={handleGenerate}
                    >
                        <Sparkles className="mr-2 h-5 w-5" /> 
                        Generate Tailored Resume
                    </Button>
                </div>
            </div>
        </div>
    );
  }

  // 2. GENERATING LOADING VIEW
  if (step === "generating") {
    return (
        <div className="h-full flex flex-col items-center justify-center space-y-8 animate-in fade-in duration-500">
            <div className="relative">
                <div className="absolute inset-0 bg-primary/20 blur-3xl rounded-full" />
                <Loader2 className="h-16 w-16 text-primary animate-spin relative z-10" />
            </div>
            <div className="text-center space-y-2 relative z-10">
                <h3 className="text-2xl font-bold">{generationStatus}</h3>
                <p className="text-muted-foreground">Please wait while we craft your document...</p>
            </div>
        </div>
    );
  }

  // 3. EDITOR / PREVIEW SPLIT VIEW (Existing Code)
  return (
    <div className="h-full flex flex-col animate-in fade-in duration-500">
        {/* Toolbar */}
        <div className="h-16 border-b bg-background flex items-center justify-between px-6 shrink-0">
            <div className="flex items-center gap-4">
                <Button variant="ghost" onClick={() => setStep("setup")}>← Start Over</Button>
                <div className="h-6 w-px bg-border" />
                <span className="font-semibold text-sm">Template: {TEMPLATES.find(t => t.id === selectedTemplate)?.name}</span>
                {jdFile && <Badge variant="secondary" className="text-xs font-normal">Tailored to Job</Badge>}
            </div>
            <div className="flex items-center gap-2">
                <Button variant="outline" size="sm" onClick={handleCompile} disabled={isCompiling}>
                    {isCompiling ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <RefreshCw className="mr-2 h-4 w-4" />}
                    Re-Compile
                </Button>
                <Button size="sm">
                    <Download className="mr-2 h-4 w-4" /> Download PDF
                </Button>
            </div>
        </div>

        {/* Main Split Area */}
        <div className="flex-1 flex overflow-hidden">
            {/* Left: LaTeX Editor */}
            <div className="w-1/2 flex flex-col border-r bg-slate-950 text-slate-50">
                <div className="p-2 border-b border-slate-800 bg-slate-900 text-xs text-slate-400 flex items-center gap-2">
                    <Code className="h-3 w-3" /> main.tex
                </div>
                <textarea 
                    className="flex-1 w-full bg-slate-950 p-4 font-mono text-sm focus:outline-none resize-none leading-relaxed"
                    value={latexCode}
                    onChange={(e) => setLatexCode(e.target.value)}
                    spellCheck={false}
                />
            </div>

            {/* Right: PDF Preview */}
            <div className="w-1/2 bg-slate-100 flex flex-col items-center justify-center p-8 overflow-y-auto">
                <div className="bg-white shadow-2xl w-full max-w-[600px] aspect-[1/1.414] min-h-[800px] p-12 text-slate-900 text-sm transform scale-90 origin-top">
                    {/* Mock Rendered HTML Content based on LaTeX */}
                    <div className="space-y-6">
                        <div className="border-b pb-4">
                            <h1 className="text-3xl font-bold uppercase tracking-wide">Haosen Shi</h1>
                            <p className="text-slate-600 mt-1">Software Engineer • demo@cvibe.ai • (555) 123-4567</p>
                        </div>
                        
                        <div>
                            <h3 className="text-lg font-bold uppercase border-b border-slate-300 mb-2 pb-1">Education</h3>
                            <div className="flex justify-between">
                                <span className="font-bold">Stanford University</span>
                                <span>2022 - 2024</span>
                            </div>
                            <p>Master of Computer Science</p>
                        </div>

                        <div>
                            <h3 className="text-lg font-bold uppercase border-b border-slate-300 mb-2 pb-1">Experience</h3>
                            <div className="flex justify-between mb-1">
                                <span className="font-bold">Google - Software Engineering Intern</span>
                                <span>Summer 2023</span>
                            </div>
                            <ul className="list-disc list-inside space-y-1 ml-2">
                                <li>Optimized distributed storage system latency by 20%.</li>
                                <li>Implemented new React components for internal dashboard.</li>
                            </ul>
                        </div>

                         <div>
                            <h3 className="text-lg font-bold uppercase border-b border-slate-300 mb-2 pb-1">Skills</h3>
                            <p>Java, Python, TypeScript, React, Next.js, Kubernetes</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
  );
}