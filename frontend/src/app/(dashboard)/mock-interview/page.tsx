"use client";

import { useState, useRef, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Bot, User, Send, Loader2, Mic } from "lucide-react";
import { cn } from "@/lib/utils";

type Message = {
  id: number;
  role: "ai" | "user";
  content: string;
};

type InterviewConfig = {
  company: string;
  position: string;
  resumeId: string;
};

export default function MockInterviewPage() {
  const [config, setConfig] = useState<InterviewConfig | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  // Configuration State
  const [companyInput, setCompanyInput] = useState("");
  const [positionInput, setPositionInput] = useState("");
  const [resumeSelect, setResumeSelect] = useState("v2.0");

  const startInterview = () => {
    if (!companyInput || !positionInput) return;

    const newConfig = {
        company: companyInput,
        position: positionInput,
        resumeId: resumeSelect
    };
    setConfig(newConfig);
    
    // Initial AI Message
    setMessages([{
        id: 1,
        role: "ai",
        content: `Hello. I am the hiring manager at **${newConfig.company}**. Thank you for applying for the **${newConfig.position}** role. 

I've reviewed your resume (${newConfig.resumeId}). Could you briefly introduce yourself and explain why you are interested in this position?`
    }]);
  };

  // Auto-scroll
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  const handleSend = async () => {
    if (!input.trim()) return;

    const userMsg: Message = {
      id: Date.now(),
      role: "user",
      content: input,
    };

    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setIsLoading(true);

    // Mock AI Response
    setTimeout(() => {
      const aiMsg: Message = {
        id: Date.now() + 1,
        role: "ai",
        content: "That's a good start. Based on your experience with React mentioned in your resume, can you describe a challenging technical problem you solved recently?",
      };
      setMessages((prev) => [...prev, aiMsg]);
      setIsLoading(false);
    }, 2000);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  // 1. CONFIGURATION VIEW
  if (!config) {
    return (
        <div className="flex h-full items-center justify-center p-8 animate-in fade-in zoom-in duration-500">
            <Card className="w-full max-w-lg border-0 bg-card shadow-2xl">
                <CardHeader>
                    <CardTitle className="text-2xl text-center">Setup Mock Interview</CardTitle>
                    <CardDescription className="text-center">
                        Configure the simulation parameters.
                    </CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                    <div className="space-y-2">
                        <Label>Target Company</Label>
                        <Input 
                            placeholder="e.g. Google, Amazon, StartupX" 
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
                            className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                            value={resumeSelect}
                            onChange={(e) => setResumeSelect(e.target.value)}
                        >
                            <option value="v1.0">Resume v1.0 (Original)</option>
                            <option value="v2.0">Resume v2.0 (AI Polished)</option>
                            <option value="v2.1">Resume v2.1 (Google Tailored)</option>
                        </select>
                    </div>
                    <Button 
                        className="w-full h-12 text-lg font-semibold" 
                        onClick={startInterview}
                        disabled={!companyInput || !positionInput}
                    >
                        Start Interview Simulation
                    </Button>
                </CardContent>
            </Card>
        </div>
    );
  }

  // 2. INTERVIEW CHAT VIEW
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
                    <h2 className="font-semibold">{config.company} Interviewer</h2>
                    <p className="text-xs text-muted-foreground">Role: {config.position}</p>
                </div>
            </div>
            <Button variant="outline" size="sm" onClick={() => setConfig(null)}>
                End Session
            </Button>
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
              {/* Avatar */}
              <div className={cn(
                  "h-8 w-8 rounded-full flex items-center justify-center shrink-0",
                  msg.role === "ai" ? "bg-red-500/10 text-red-500" : "bg-muted text-muted-foreground"
              )}>
                {msg.role === "ai" ? <Bot className="h-5 w-5" /> : <User className="h-5 w-5" />}
              </div>

              {/* Bubble */}
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
