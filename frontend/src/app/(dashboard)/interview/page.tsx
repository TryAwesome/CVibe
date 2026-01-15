"use client";

import { useState, useRef, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import { Send, Bot, User, Loader2, Plus, Clock, CheckCircle, Pause, Play, Trash2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuth } from "@/lib/contexts/auth-context";
import api, { InterviewSession, InterviewAnswer } from "@/lib/api";

type Message = {
  id: number;
  role: "ai" | "user";
  content: string;
  questionId?: number;
};

export default function InterviewPage() {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const [sessions, setSessions] = useState<InterviewSession[]>([]);
  const [activeSession, setActiveSession] = useState<InterviewSession | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [loadingSessions, setLoadingSessions] = useState(true);
  const [currentQuestionId, setCurrentQuestionId] = useState<number | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  // Fetch sessions on mount
  useEffect(() => {
    if (isAuthenticated) {
      loadSessions();
    }
  }, [isAuthenticated]);

  const loadSessions = async () => {
    try {
      setLoadingSessions(true);
      const res = await api.getInterviewSessions();
      if (res.success && res.data) {
        setSessions(res.data);
      }
    } catch (error) {
      console.error("Failed to load sessions:", error);
    } finally {
      setLoadingSessions(false);
    }
  };

  const loadSessionAnswers = async (sessionId: string) => {
    try {
      const res = await api.getAnswers(sessionId);
      if (!res.success || !res.data) return;
      const answers = res.data;
      // Convert answers to messages
      const msgs: Message[] = [];
      answers.forEach((answer, index) => {
        msgs.push({
          id: index * 2 + 1,
          role: "ai",
          content: answer.question || `Question ${index + 1}`,
          questionId: answer.id,
        });
        if (answer.answer) {
          msgs.push({
            id: index * 2 + 2,
            role: "user",
            content: answer.answer,
          });
        }
      });
      setMessages(msgs);
      // Set current question to the last unanswered one
      const unanswered = answers.find(a => !a.answer);
      if (unanswered) {
        setCurrentQuestionId(unanswered.id);
      }
    } catch (error) {
      console.error("Failed to load answers:", error);
    }
  };

  const startNewSession = async () => {
    try {
      setIsLoading(true);
      const res = await api.createInterviewSession({ topic: "Profile Building" });
      if (!res.success || !res.data) return;
      const session = res.data;
      setSessions(prev => [session, ...prev]);
      setActiveSession(session);
      setMessages([{
        id: 1,
        role: "ai",
        content: "Hello! I'm your CVibe AI Assistant. My goal is to help you build a comprehensive profile database. Let's start with your **Education**. \n\nCould you tell me which university you attended and what was your major?",
      }]);
    } catch (error) {
      console.error("Failed to create session:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const selectSession = async (session: InterviewSession) => {
    setActiveSession(session);
    await loadSessionAnswers(session.id);
  };

  const pauseSession = async () => {
    if (!activeSession) return;
    try {
      await api.pauseInterviewSession(String(activeSession.id));
      setActiveSession({ ...activeSession, status: "paused" });
      setSessions(prev => prev.map(s => s.id === activeSession.id ? { ...s, status: "paused" } : s));
    } catch (error) {
      console.error("Failed to pause session:", error);
    }
  };

  const resumeSession = async () => {
    if (!activeSession) return;
    try {
      await api.resumeInterviewSession(String(activeSession.id));
      setActiveSession({ ...activeSession, status: "active" });
      setSessions(prev => prev.map(s => s.id === activeSession.id ? { ...s, status: "active" } : s));
    } catch (error) {
      console.error("Failed to resume session:", error);
    }
  };

  const deleteSession = async (sessionId: string) => {
    try {
      await api.deleteInterviewSession(sessionId);
      setSessions(prev => prev.filter(s => s.id !== sessionId));
      if (activeSession?.id === sessionId) {
        setActiveSession(null);
        setMessages([]);
      }
    } catch (error) {
      console.error("Failed to delete session:", error);
    }
  };

  // Auto-scroll to bottom
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  const handleSend = async () => {
    if (!input.trim() || !activeSession) return;

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
        await api.submitAnswer(String(activeSession.id), { questionId: String(currentQuestionId), answer: input });
      }

      // Get next question from AI (mock for now, would be real AI response)
      setTimeout(() => {
        const aiMsg: Message = {
          id: Date.now() + 1,
          role: "ai",
          content: generateFollowUpQuestion(messages.length),
        };
        setMessages((prev) => [...prev, aiMsg]);
        setIsLoading(false);
      }, 1500);
    } catch (error) {
      console.error("Failed to submit answer:", error);
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  if (authLoading || loadingSessions) {
    return (
      <div className="h-full flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="h-full flex items-center justify-center">
        <p className="text-muted-foreground">Please log in to access the interview assistant</p>
      </div>
    );
  }

  return (
    <div className="h-full flex w-full p-4 gap-4">
      {/* Sessions Sidebar */}
      <div className="w-80 flex flex-col gap-4">
        <Button onClick={startNewSession} disabled={isLoading} className="w-full">
          <Plus className="h-4 w-4 mr-2" />
          New Session
        </Button>
        
        <div className="flex-1 overflow-y-auto space-y-2">
          {sessions.length === 0 ? (
            <Card className="p-4 text-center text-muted-foreground text-sm">
              No sessions yet. Click the button above to start.
            </Card>
          ) : (
            sessions.map((session) => (
              <Card
                key={session.id}
                className={cn(
                  "p-3 cursor-pointer transition-colors hover:bg-muted/50",
                  activeSession?.id === session.id && "border-primary bg-primary/5"
                )}
                onClick={() => selectSession(session)}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    {session.status === "completed" ? (
                      <CheckCircle className="h-4 w-4 text-green-500" />
                    ) : session.status === "paused" ? (
                      <Pause className="h-4 w-4 text-yellow-500" />
                    ) : (
                      <Clock className="h-4 w-4 text-blue-500" />
                    )}
                    <span className="font-medium text-sm">{session.topic || "Profile Building"}</span>
                  </div>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-6 w-6"
                    onClick={(e) => {
                      e.stopPropagation();
                      deleteSession(session.id);
                    }}
                  >
                    <Trash2 className="h-3 w-3" />
                  </Button>
                </div>
                <p className="text-xs text-muted-foreground mt-1">
                  {new Date(session.createdAt).toLocaleDateString()}
                </p>
              </Card>
            ))
          )}
        </div>
      </div>

      {/* Chat Area */}
      <div className="flex-1 overflow-hidden rounded-xl border bg-background shadow-sm flex flex-col h-full">
        {!activeSession ? (
          <div className="flex-1 flex items-center justify-center text-muted-foreground">
            <div className="text-center">
              <Bot className="h-12 w-12 mx-auto mb-4 text-muted-foreground/50" />
              <p>Select a session or create a new one to begin</p>
            </div>
          </div>
        ) : (
          <>
            {/* Header */}
            <div className="p-4 border-b flex items-center justify-between bg-muted/20">
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                  <Bot className="h-6 w-6 text-primary" />
                </div>
                <div>
                  <h2 className="font-semibold">Profile Builder AI</h2>
                  <p className="text-xs text-muted-foreground">
                    Topic: {activeSession.topic || "Education & Experience"}
                  </p>
                </div>
              </div>
              <div className="flex gap-2">
                {activeSession.status === "active" ? (
                  <Button variant="outline" size="sm" onClick={pauseSession}>
                    <Pause className="h-4 w-4 mr-1" /> Pause
                  </Button>
                ) : activeSession.status === "paused" ? (
                  <Button variant="outline" size="sm" onClick={resumeSession}>
                    <Play className="h-4 w-4 mr-1" /> Resume
                  </Button>
                ) : null}
              </div>
            </div>

            {/* Chat Messages */}
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
                    msg.role === "ai" ? "bg-primary/10 text-primary" : "bg-muted text-muted-foreground"
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
                  <div className="h-8 w-8 rounded-full bg-primary/10 text-primary flex items-center justify-center shrink-0">
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
                <Input
                  placeholder="Type your answer..."
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKeyDown}
                  className="flex-1 bg-background"
                  disabled={isLoading || activeSession.status !== "active"}
                />
                <Button 
                  onClick={handleSend} 
                  disabled={isLoading || !input.trim() || activeSession.status !== "active"} 
                  size="icon"
                >
                  <Send className="h-4 w-4" />
                </Button>
              </div>
              <p className="text-[10px] text-center text-muted-foreground mt-2">
                AI may make mistakes. Please verify important information.
              </p>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

// Follow-up question generator (would be replaced by real AI in production)
function generateFollowUpQuestion(step: number): string {
  const responses = [
    "Got it. What was your **GPA** (Grade Point Average)?",
    "Impressive. Did you take any specific courses related to **Data Structures** or **Algorithms**?",
    "Okay. Now let's move on to your **Internships**. Have you completed any internships recently?",
    "What was your main responsibility during that internship?",
    "Did you use any specific technologies like **React**, **Python**, or **AWS**?",
    "Great details! I've updated your profile database. Is there anything else you'd like to add regarding your experience?",
  ];
  return responses[step % responses.length];
}
