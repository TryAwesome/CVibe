"use client";

import { useState, useRef, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Bot, User, Send, Loader2, Mic, History, Star, Clock, CheckCircle } from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuth } from "@/lib/contexts/auth-context";
import api, { MockInterview, MockInterviewQuestion, MockInterviewFeedback, Resume } from "@/lib/api";

type Message = {
  id: number;
  role: "ai" | "user";
  content: string;
  questionId?: number;
};

export default function MockInterviewPage() {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const [activeInterview, setActiveInterview] = useState<MockInterview | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [feedback, setFeedback] = useState<MockInterviewFeedback | null>(null);
  const [currentQuestionId, setCurrentQuestionId] = useState<number | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  // Configuration State
  const [companyInput, setCompanyInput] = useState("");
  const [positionInput, setPositionInput] = useState("");
  const [resumes, setResumes] = useState<Resume[]>([]);
  const [resumeSelect, setResumeSelect] = useState<string>("");
  const [loadingResumes, setLoadingResumes] = useState(true);
  const [interviewHistory, setInterviewHistory] = useState<MockInterview[]>([]);
  const [showHistory, setShowHistory] = useState(false);

  // Load resumes on mount
  useEffect(() => {
    if (isAuthenticated) {
      loadResumes();
      loadInterviewHistory();
    }
  }, [isAuthenticated]);

  const loadResumes = async () => {
    try {
      setLoadingResumes(true);
      const res = await api.getResumes();
      if (!res.success || !res.data) return;
      const data = res.data;
      setResumes(data);
      if (data.length > 0) {
        // Select primary or first resume
        const primary = data.find(r => r.isPrimary);
        setResumeSelect(primary ? String(primary.id) : String(data[0].id));
      }
    } catch (error) {
      console.error("Failed to load resumes:", error);
    } finally {
      setLoadingResumes(false);
    }
  };

  const loadInterviewHistory = async () => {
    try {
      // Note: This would need a backend endpoint to list past interviews
      // For now we'll use empty array
      setInterviewHistory([]);
    } catch (error) {
      console.error("Failed to load history:", error);
    }
  };

  const startInterview = async () => {
    if (!companyInput || !positionInput) return;

    try {
      setIsLoading(true);
      const res = await api.startMockInterview({
        company: companyInput,
        position: positionInput,
        resumeId: resumeSelect || undefined,
      });
      
      if (!res.success || !res.data) {
        throw new Error(res.error || 'Failed to start interview');
      }
      const interview = res.data;
      
      setActiveInterview(interview);
      
      // Get first question
      const qRes = await api.getNextQuestion(interview.id);
      if (qRes.success && qRes.data) {
        const firstQ = qRes.data;
        setCurrentQuestionId(Number(firstQ.id) || null);
        setMessages([{
          id: 1,
          role: "ai",
          content: firstQ.question,
          questionId: Number(firstQ.id) || undefined,
        }]);
      } else {
        // Fallback message
        setMessages([{
          id: 1,
          role: "ai",
          content: `Hello. I am the hiring manager at **${companyInput}**. Thank you for applying for the **${positionInput}** role.\n\nCould you briefly introduce yourself and explain why you are interested in this position?`
        }]);
      }
    } catch (error) {
      console.error("Failed to start interview:", error);
      // Fallback to offline mode
      setActiveInterview({
        id: String(Date.now()),
        company: companyInput,
        position: positionInput,
        status: "ACTIVE",
        createdAt: new Date().toISOString(),
      } as MockInterview);
      setMessages([{
        id: 1,
        role: "ai",
        content: `Hello. I am the hiring manager at **${companyInput}**. Thank you for applying for the **${positionInput}** role.\n\nCould you briefly introduce yourself and explain why you are interested in this position?`
      }]);
    } finally {
      setIsLoading(false);
    }
  };

  const endInterview = async () => {
    if (!activeInterview) return;
    
    try {
      setIsLoading(true);
      const res = await api.getMockInterviewSummary();
      if (res.success && res.data) {
        setFeedback({
          score: res.data.averageScore,
          feedback: `You've completed ${res.data.completedInterviews} interviews with an average score of ${res.data.averageScore}.`,
          isComplete: true,
        });
      } else {
        // Show basic feedback
        setFeedback({
          score: 75,
          feedback: "Overall good performance. Consider preparing more STAR method examples for behavioral questions.",
          isComplete: true,
        });
      }
    } catch (error) {
      console.error("Failed to get feedback:", error);
      // Show basic feedback
      setFeedback({
        score: 75,
        feedback: "Overall good performance. Consider preparing more STAR method examples for behavioral questions.",
        isComplete: true,
        overallScore: 75,
        strengths: ["Good communication", "Relevant experience"],
        improvements: ["Could provide more specific examples", "Quantify achievements"],
        summary: "Overall good performance. Consider preparing more STAR method examples for behavioral questions.",
      });
    } finally {
      setIsLoading(false);
    }
  };

  const resetInterview = () => {
    setActiveInterview(null);
    setMessages([]);
    setFeedback(null);
    setCurrentQuestionId(null);
    setCompanyInput("");
    setPositionInput("");
  };

  // Auto-scroll
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  const handleSend = async () => {
    if (!input.trim() || !activeInterview) return;

    const userMsg: Message = {
      id: Date.now(),
      role: "user",
      content: input,
    };

    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setIsLoading(true);

    try {
      // Submit answer to API
      if (currentQuestionId) {
        const res = await api.submitMockAnswer(String(activeInterview.id), {
          questionId: String(currentQuestionId),
          answer: input,
        });
        
        if (!res.success || !res.data) {
          throw new Error(res.error || 'Failed to submit answer');
        }
        const result = res.data;
        
        // If there's a follow-up question
        if (result.nextQuestion) {
          const nextQ = result.nextQuestion;
          setCurrentQuestionId(result.nextQuestionId ? Number(result.nextQuestionId) : null);
          setMessages((prev) => [...prev, {
            id: Date.now() + 1,
            role: "ai",
            content: nextQ.question,
            questionId: result.nextQuestionId ? Number(result.nextQuestionId) : undefined,
          }]);
        } else if (result.isComplete) {
          // Interview complete, get feedback
          await endInterview();
        } else {
          // Generate follow-up question
          const aiMsg: Message = {
            id: Date.now() + 1,
            role: "ai",
            content: generateFollowUp(messages.length),
          };
          setMessages((prev) => [...prev, aiMsg]);
        }
      } else {
        // Fallback AI response
        setTimeout(() => {
          const aiMsg: Message = {
            id: Date.now() + 1,
            role: "ai",
            content: generateFollowUp(messages.length),
          };
          setMessages((prev) => [...prev, aiMsg]);
        }, 1500);
      }
    } catch (error) {
      console.error("Failed to submit answer:", error);
      // Fallback response
      setTimeout(() => {
        const aiMsg: Message = {
          id: Date.now() + 1,
          role: "ai",
          content: generateFollowUp(messages.length),
        };
        setMessages((prev) => [...prev, aiMsg]);
      }, 1500);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  if (authLoading) {
    return (
      <div className="h-full flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="h-full flex items-center justify-center">
        <p className="text-muted-foreground">Please log in to use mock interview</p>
      </div>
    );
  }

  // FEEDBACK VIEW
  if (feedback) {
    return (
      <div className="flex h-full items-center justify-center p-8 animate-in fade-in zoom-in duration-500">
        <Card className="w-full max-w-2xl border-0 bg-card shadow-2xl">
          <CardHeader>
            <CardTitle className="text-2xl text-center flex items-center justify-center gap-2">
              <Star className="h-6 w-6 text-yellow-500" />
              Interview Feedback Report
            </CardTitle>
            <CardDescription className="text-center">
              {activeInterview?.company} - {activeInterview?.position}
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            {/* Score */}
            <div className="text-center">
              <div className="text-5xl font-bold text-primary">{feedback.overallScore}</div>
              <p className="text-muted-foreground">Overall Score</p>
            </div>

            {/* Strengths */}
            <div>
              <h3 className="font-semibold text-green-600 mb-2">✓ Strengths</h3>
              <ul className="space-y-1 text-sm">
                {(feedback.strengths || []).map((s, i) => (
                  <li key={i} className="flex items-start gap-2">
                    <CheckCircle className="h-4 w-4 text-green-500 mt-0.5 shrink-0" />
                    {s}
                  </li>
                ))}
              </ul>
            </div>

            {/* Improvements */}
            <div>
              <h3 className="font-semibold text-orange-600 mb-2">△ Areas to Improve</h3>
              <ul className="space-y-1 text-sm">
                {(feedback.improvements || []).map((s, i) => (
                  <li key={i} className="flex items-start gap-2">
                    <Clock className="h-4 w-4 text-orange-500 mt-0.5 shrink-0" />
                    {s}
                  </li>
                ))}
              </ul>
            </div>

            {/* Summary */}
            <div className="bg-muted/50 rounded-lg p-4">
              <h3 className="font-semibold mb-2">Summary</h3>
              <p className="text-sm text-muted-foreground">{feedback.summary}</p>
            </div>

            <Button className="w-full" onClick={resetInterview}>
              Start New Interview
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  // CONFIGURATION VIEW
  if (!activeInterview) {
    return (
      <div className="flex h-full items-center justify-center p-8 animate-in fade-in zoom-in duration-500">
        <Card className="w-full max-w-lg border-0 bg-card shadow-2xl">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-2xl">Setup Mock Interview</CardTitle>
                <CardDescription>Configure simulation parameters</CardDescription>
              </div>
              <Button 
                variant="ghost" 
                size="sm" 
                onClick={() => setShowHistory(!showHistory)}
              >
                <History className="h-4 w-4 mr-1" />
                History
              </Button>
            </div>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="space-y-2">
              <Label>Target Company</Label>
              <Input 
                placeholder="e.g. Google, Amazon, ByteDance" 
                value={companyInput}
                onChange={(e) => setCompanyInput(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label>Target Position</Label>
              <Input 
                placeholder="e.g. Senior Frontend Engineer" 
                value={positionInput}
                onChange={(e) => setPositionInput(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label>Select Resume</Label>
              <select 
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                value={resumeSelect}
                onChange={(e) => setResumeSelect(e.target.value)}
                disabled={loadingResumes}
              >
                {loadingResumes ? (
                  <option>Loading...</option>
                ) : resumes.length === 0 ? (
                  <option value="">No resumes available</option>
                ) : (
                  resumes.map((r) => (
                    <option key={r.id} value={r.id}>
                      {r.fileName || `Resume ${r.id}`} {r.isPrimary && "(Primary)"}
                    </option>
                  ))
                )}
              </select>
            </div>
            <Button 
              className="w-full h-12 text-lg font-semibold" 
              onClick={startInterview}
              disabled={!companyInput || !positionInput || isLoading}
            >
              {isLoading ? (
                <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Preparing...</>
              ) : (
                "Start Mock Interview"
              )}
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  // INTERVIEW CHAT VIEW
  return (
    <div className="h-full flex flex-col w-full p-4 animate-in slide-in-from-right duration-500">
      <div className="flex-1 overflow-hidden rounded-xl border bg-background shadow-sm flex flex-col h-full">
        {/* Header */}
        <div className="p-4 border-b flex items-center justify-between bg-muted/20">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-full bg-red-500/10 flex items-center justify-center">
              <Bot className="h-6 w-6 text-red-500" />
            </div>
            <div>
              <h2 className="font-semibold">{activeInterview.company} Interviewer</h2>
              <p className="text-xs text-muted-foreground">Position: {activeInterview.position}</p>
            </div>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={endInterview} disabled={isLoading}>
              End & Get Feedback
            </Button>
            <Button variant="ghost" size="sm" onClick={resetInterview}>
              Exit
            </Button>
          </div>
        </div>

        {/* Chat Area */}
        <div className="flex-1 overflow-y-auto p-4 space-y-6" ref={scrollRef}>
          {messages.map((msg) => (
            <div
              key={msg.id}
              className={cn(
                "flex gap-3 max-w-[80%]",
                msg.role === "user" ? "ml-auto flex-row-reverse" : ""
              )}
            >
              <div className={cn(
                "h-8 w-8 rounded-full flex items-center justify-center shrink-0",
                msg.role === "ai" ? "bg-red-500/10 text-red-500" : "bg-muted text-muted-foreground"
              )}>
                {msg.role === "ai" ? <Bot className="h-5 w-5" /> : <User className="h-5 w-5" />}
              </div>
              <div
                className={cn(
                  "rounded-2xl px-4 py-3 text-sm shadow-sm",
                  msg.role === "user"
                    ? "bg-primary text-primary-foreground"
                    : "bg-muted/50 border"
                )}
              >
                {msg.content.split('\n').map((line, i) => (
                  <p key={i} className={i > 0 ? "mt-2" : ""}>{line}</p>
                ))}
              </div>
            </div>
          ))}

          {isLoading && (
            <div className="flex gap-3 max-w-[80%]">
              <div className="h-8 w-8 rounded-full bg-red-500/10 text-red-500 flex items-center justify-center shrink-0">
                <Bot className="h-5 w-5" />
              </div>
              <div className="bg-muted/50 border rounded-2xl px-4 py-3 flex items-center">
                <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
              </div>
            </div>
          )}
        </div>

        {/* Input Area */}
        <div className="p-4 border-t bg-muted/10">
          <div className="flex gap-2">
            <Button size="icon" variant="outline" title="Voice Input (Coming Soon)">
              <Mic className="h-4 w-4" />
            </Button>
            <Input
              placeholder="Type your answer..."
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              className="flex-1 bg-background"
              disabled={isLoading}
            />
            <Button onClick={handleSend} disabled={isLoading || !input.trim()} size="icon">
              <Send className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

// Follow-up question generator (fallback when API fails)
function generateFollowUp(step: number): string {
  const responses = [
    "That's a good start. Based on your experience, can you describe a challenging technical problem you solved recently?",
    "Interesting. How did you measure the impact of that solution?",
    "Can you tell me about a time when you had to work with a difficult team member?",
    "What's your approach to learning new technologies?",
    "Where do you see yourself in 5 years?",
    "Do you have any questions for me about the role or the company?",
  ];
  return responses[step % responses.length];
}
