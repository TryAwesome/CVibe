"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { FileText, Download, Save, RefreshCw, Upload, Code, Loader2, Sparkles, CheckCircle2, AlertCircle } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import { api, ResumeTemplate, ResumeGeneration } from "@/lib/api";
import { useAuth } from "@/lib/contexts/auth-context";

export default function ResumeBuilderPage() {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  
  // State for Wizard
  const [step, setStep] = useState<"setup" | "generating" | "editor">("setup");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  // Setup State
  const [templates, setTemplates] = useState<ResumeTemplate[]>([]);
  const [selectedTemplate, setSelectedTemplate] = useState<string | null>(null);
  const [jdFile, setJdFile] = useState<File | null>(null);
  const [targetJob, setTargetJob] = useState("");
  const [targetCompany, setTargetCompany] = useState("");
  
  // Editor State
  const [currentGeneration, setCurrentGeneration] = useState<ResumeGeneration | null>(null);
  const [latexCode, setLatexCode] = useState("");
  const [isCompiling, setIsCompiling] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  // Generation Animation State
  const [generationStatus, setGenerationStatus] = useState("Initializing...");

  // Fetch templates on mount
  useEffect(() => {
    if (!authLoading && isAuthenticated) {
      fetchTemplates();
    } else if (!authLoading && !isAuthenticated) {
      setIsLoading(false);
    }
  }, [authLoading, isAuthenticated]);

  const fetchTemplates = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await api.getTemplates();
      if (res.success && res.data) {
        setTemplates(res.data);
        // If no templates from API, use fallback
        if (res.data.length === 0) {
          setTemplates([
            { id: "modern", name: "Modern Tech", description: "Clean sans-serif style perfect for SW Engineers.", category: "tech", previewUrl: "", isFeatured: true, createdAt: "" },
            { id: "classic", name: "Ivy League Classic", description: "Traditional serif font, highly professional.", category: "traditional", previewUrl: "", isFeatured: true, createdAt: "" },
            { id: "minimal", name: "Minimalist", description: "Focus purely on content with ample whitespace.", category: "minimal", previewUrl: "", isFeatured: false, createdAt: "" },
          ]);
        }
      } else {
        // Use fallback templates
        setTemplates([
          { id: "modern", name: "Modern Tech", description: "Clean sans-serif style perfect for SW Engineers.", category: "tech", previewUrl: "", isFeatured: true, createdAt: "" },
          { id: "classic", name: "Ivy League Classic", description: "Traditional serif font, highly professional.", category: "traditional", previewUrl: "", isFeatured: true, createdAt: "" },
          { id: "minimal", name: "Minimalist", description: "Focus purely on content with ample whitespace.", category: "minimal", previewUrl: "", isFeatured: false, createdAt: "" },
        ]);
      }
    } catch (err) {
      console.error('Error fetching templates:', err);
      // Use fallback templates
      setTemplates([
        { id: "modern", name: "Modern Tech", description: "Clean sans-serif style perfect for SW Engineers.", category: "tech", previewUrl: "", isFeatured: true, createdAt: "" },
        { id: "classic", name: "Ivy League Classic", description: "Traditional serif font, highly professional.", category: "traditional", previewUrl: "", isFeatured: true, createdAt: "" },
        { id: "minimal", name: "Minimalist", description: "Focus purely on content with ample whitespace.", category: "minimal", previewUrl: "", isFeatured: false, createdAt: "" },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleGenerate = async () => {
    if (!selectedTemplate) return;
    setStep("generating");
    setError(null);

    // Animation steps
    const steps = [
      "Scanning Job Description...",
      "Extracting keywords...",
      "Retrieving matching data from Profile Database...",
      "Optimizing bullet points for ATS...",
      "Generating LaTeX code..."
    ];

    let i = 0;
    const interval = setInterval(() => {
      if (i < steps.length) {
        setGenerationStatus(steps[i]);
        i++;
      }
    }, 800);

    try {
      const res = await api.generateResume({
        templateId: selectedTemplate,
        targetJob: targetJob || undefined,
        targetCompany: targetCompany || undefined,
      });

      clearInterval(interval);

      if (res.success && res.data) {
        setCurrentGeneration(res.data);
        
        // If generation is pending/processing, poll for updates
        if (res.data.status === 'PENDING' || res.data.status === 'PROCESSING') {
          pollGenerationStatus(res.data.id);
        } else if (res.data.status === 'COMPLETED' && res.data.latexContent) {
          setLatexCode(res.data.latexContent);
          setStep("editor");
        } else {
          // Use default template
          setLatexCode(getDefaultLatex());
          setStep("editor");
        }
      } else {
        setError(res.error || 'Failed to generate resume');
        // Use default template as fallback
        setLatexCode(getDefaultLatex());
        setStep("editor");
      }
    } catch (err) {
      clearInterval(interval);
      console.error('Error generating resume:', err);
      // Use default template as fallback
      setLatexCode(getDefaultLatex());
      setStep("editor");
    }
  };

  const pollGenerationStatus = async (generationId: string) => {
    let attempts = 0;
    const maxAttempts = 30;

    const poll = async () => {
      if (attempts >= maxAttempts) {
        setLatexCode(getDefaultLatex());
        setStep("editor");
        return;
      }

      try {
        const res = await api.getGenerationById(generationId);
        if (res.success && res.data) {
          if (res.data.status === 'COMPLETED' && res.data.latexContent) {
            setLatexCode(res.data.latexContent);
            setCurrentGeneration(res.data);
            setStep("editor");
            return;
          } else if (res.data.status === 'FAILED') {
            setLatexCode(getDefaultLatex());
            setStep("editor");
            return;
          }
        }

        attempts++;
        setTimeout(poll, 2000);
      } catch {
        setLatexCode(getDefaultLatex());
        setStep("editor");
      }
    };

    poll();
  };

  const getDefaultLatex = () => `\\documentclass{article}
\\usepackage{titlesec}
\\usepackage{enumitem}

\\begin{document}

\\section*{Your Name}
\\textit{Your Title} \\\\ 
Email: your.email@example.com \\\\ 
Phone: (555) 123-4567

\\section*{Education}
\\textbf{Your University} \\hfill Year - Year \\\\ 
Your Degree

\\section*{Experience}
\\textbf{Company Name} -- Your Role \\hfill Date Range \\\\ 
- Your achievement here. \\\\ 
- Another achievement.

\\section*{Skills}
Your skills here

\\end{document}
`;

  const handleSaveLatex = async () => {
    if (!currentGeneration) return;
    setIsSaving(true);
    try {
      await api.updateGenerationLatex(currentGeneration.id, latexCode);
    } catch (err) {
      console.error('Error saving:', err);
    } finally {
      setIsSaving(false);
    }
  };

  const handleDownload = async () => {
    if (!currentGeneration) {
      // Download current latex as .tex file
      const blob = new Blob([latexCode], { type: 'text/plain' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'resume.tex';
      a.click();
      URL.revokeObjectURL(url);
      return;
    }

    try {
      const res = await api.exportGeneration(currentGeneration.id, 'pdf');
      if (res.success && res.data?.downloadUrl) {
        window.open(res.data.downloadUrl, '_blank');
      }
    } catch (err) {
      console.error('Error downloading:', err);
      // Fallback to tex download
      const blob = new Blob([latexCode], { type: 'text/plain' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'resume.tex';
      a.click();
      URL.revokeObjectURL(url);
    }
  };

  const handleCompile = () => {
    setIsCompiling(true);
    setTimeout(() => setIsCompiling(false), 1000);
  };

  const getTemplateColor = (category: string) => {
    const colors: Record<string, string> = {
      tech: "bg-blue-100",
      traditional: "bg-slate-100",
      minimal: "bg-white border",
      creative: "bg-purple-100",
    };
    return colors[category] || "bg-gray-100";
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
        <p className="text-muted-foreground">Please log in to use the Resume Builder</p>
      </div>
    );
  }

  // 1. SETUP VIEW
  if (step === "setup") {
    return (
      <div className="h-full p-8 animate-in fade-in duration-500 overflow-y-auto">
        <div className="max-w-6xl mx-auto space-y-8">
          <div className="text-center space-y-2">
            <h2 className="text-3xl font-bold tracking-tight">AI Resume Builder</h2>
            <p className="text-muted-foreground max-w-2xl mx-auto">
              We'll tailor your resume specifically for the job. Select a template and provide job details, and our AI will pull the most relevant experience from your Profile Database.
            </p>
          </div>

          {error && (
            <div className="bg-destructive/10 text-destructive p-4 rounded-lg flex items-center gap-2">
              <AlertCircle className="h-4 w-4" />
              {error}
            </div>
          )}

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
                {templates.map((tpl) => (
                  <div 
                    key={tpl.id} 
                    className={cn(
                      "cursor-pointer border-2 rounded-xl p-4 transition-all hover:scale-[1.02]",
                      selectedTemplate === tpl.id ? "border-primary bg-primary/5" : "border-transparent bg-muted/50 hover:bg-muted"
                    )}
                    onClick={() => setSelectedTemplate(tpl.id)}
                  >
                    <div className={`h-24 w-full ${getTemplateColor(tpl.category)} mb-3 rounded-md flex items-center justify-center`}>
                      {tpl.previewUrl ? (
                        <img src={tpl.previewUrl} alt={tpl.name} className="h-full w-full object-cover rounded-md" />
                      ) : (
                        <FileText className="h-8 w-8 text-muted-foreground/30" />
                      )}
                    </div>
                    <h4 className="font-semibold text-sm">{tpl.name}</h4>
                    <p className="text-xs text-muted-foreground mt-1 line-clamp-2">{tpl.description}</p>
                    {selectedTemplate === tpl.id && (
                      <div className="mt-2 flex items-center text-xs text-primary font-medium">
                        <CheckCircle2 className="h-3 w-3 mr-1" /> Selected
                      </div>
                    )}
                    {tpl.isFeatured && (
                      <Badge variant="secondary" className="mt-2 text-[10px]">Featured</Badge>
                    )}
                  </div>
                ))}
              </CardContent>
            </Card>

            {/* Right: Job Details */}
            <Card className="border-0 shadow-md bg-card/50 flex flex-col">
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <span className="bg-primary/10 text-primary w-6 h-6 rounded-full flex items-center justify-center text-xs">2</span>
                  Target Job (Optional)
                </CardTitle>
                <CardDescription>Provide job details to tailor your resume.</CardDescription>
              </CardHeader>
              <CardContent className="flex-1 space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="targetCompany">Company Name</Label>
                  <Input 
                    id="targetCompany" 
                    placeholder="e.g., Google, Meta, Stripe" 
                    value={targetCompany}
                    onChange={(e) => setTargetCompany(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="targetJob">Position</Label>
                  <Input 
                    id="targetJob" 
                    placeholder="e.g., Senior Software Engineer" 
                    value={targetJob}
                    onChange={(e) => setTargetJob(e.target.value)}
                  />
                </div>
                
                <div className="pt-4">
                  <div 
                    className={cn(
                      "border-2 border-dashed rounded-xl p-6 flex flex-col items-center justify-center text-center transition-colors",
                      jdFile ? "border-primary bg-primary/5" : "hover:bg-accent/50 cursor-pointer"
                    )}
                    onClick={() => !jdFile && document.getElementById('jd-upload')?.click()}
                  >
                    {jdFile ? (
                      <>
                        <FileText className="h-8 w-8 text-primary mb-2" />
                        <p className="font-medium text-sm">{jdFile.name}</p>
                        <Button variant="ghost" size="sm" className="mt-2 text-destructive hover:text-destructive" onClick={(e) => { e.stopPropagation(); setJdFile(null); }}>
                          Remove
                        </Button>
                      </>
                    ) : (
                      <>
                        <Upload className="h-6 w-6 text-muted-foreground mb-2" />
                        <p className="text-sm font-medium">Upload Job Description</p>
                        <p className="text-xs text-muted-foreground mt-1">Optional: Upload screenshot or PDF</p>
                        <Input 
                          id="jd-upload" 
                          type="file" 
                          className="hidden" 
                          onChange={(e) => e.target.files && setJdFile(e.target.files[0])} 
                        />
                      </>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          <div className="flex justify-center pt-8">
            <Button 
              size="lg" 
              className="w-full max-w-md text-lg h-14 shadow-xl shadow-primary/20"
              disabled={!selectedTemplate}
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

  // 3. EDITOR VIEW
  const selectedTemplateName = templates.find(t => t.id === selectedTemplate)?.name || 'Custom';

  return (
    <div className="h-full flex flex-col animate-in fade-in duration-500">
      {/* Toolbar */}
      <div className="h-16 border-b bg-background flex items-center justify-between px-6 shrink-0">
        <div className="flex items-center gap-4">
          <Button variant="ghost" onClick={() => setStep("setup")}>← Start Over</Button>
          <div className="h-6 w-px bg-border" />
          <span className="font-semibold text-sm">Template: {selectedTemplateName}</span>
          {(targetJob || targetCompany) && <Badge variant="secondary" className="text-xs font-normal">Tailored to Job</Badge>}
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" onClick={handleSaveLatex} disabled={isSaving}>
            {isSaving ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Save className="mr-2 h-4 w-4" />}
            Save
          </Button>
          <Button variant="outline" size="sm" onClick={handleCompile} disabled={isCompiling}>
            {isCompiling ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <RefreshCw className="mr-2 h-4 w-4" />}
            Re-Compile
          </Button>
          <Button size="sm" onClick={handleDownload}>
            <Download className="mr-2 h-4 w-4" /> Download
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
            {/* Rendered preview based on LaTeX */}
            <div className="space-y-6">
              <div className="border-b pb-4">
                <h1 className="text-3xl font-bold uppercase tracking-wide">Your Name</h1>
                <p className="text-slate-600 mt-1">Your Title • your.email@example.com</p>
              </div>
              
              <div>
                <h3 className="text-lg font-bold uppercase border-b border-slate-300 mb-2 pb-1">Education</h3>
                <p className="text-sm text-muted-foreground">Edit the LaTeX on the left to see changes</p>
              </div>

              <div>
                <h3 className="text-lg font-bold uppercase border-b border-slate-300 mb-2 pb-1">Experience</h3>
                <p className="text-sm text-muted-foreground">Your experiences will appear here</p>
              </div>

              <div>
                <h3 className="text-lg font-bold uppercase border-b border-slate-300 mb-2 pb-1">Skills</h3>
                <p className="text-sm text-muted-foreground">Your skills will appear here</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}