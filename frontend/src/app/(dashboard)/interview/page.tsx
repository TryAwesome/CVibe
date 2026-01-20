"use client";

import { useState, useRef, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Card } from "@/components/ui/card";
import { Send, Bot, User, Loader2, Plus, Clock, CheckCircle, Pause, Play, Trash2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuth } from "@/lib/contexts/auth-context";
import api, { InterviewSession, ProfileInterviewStartResponse } from "@/lib/api";

type Message = {
  id: number;
  role: "ai" | "user";
  content: string;
};

type ProfileSession = {
  id: string;
  sessionId: string;
  status: "active" | "paused" | "completed";
  currentPhase: string;
  phaseName: string;
  createdAt: string;
};

export default function InterviewPage() {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const [sessions, setSessions] = useState<ProfileSession[]>([]);
  const [activeSession, setActiveSession] = useState<ProfileSession | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [loadingSessions, setLoadingSessions] = useState(true);
  const scrollRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Load sessions from localStorage on mount
  useEffect(() => {
    if (isAuthenticated) {
      loadSessions();
    }
  }, [isAuthenticated]);

  const loadSessions = async () => {
    try {
      setLoadingSessions(true);
      // Load profile interview sessions from localStorage
      const savedSessions = localStorage.getItem('profileInterviewSessions');
      if (savedSessions) {
        setSessions(JSON.parse(savedSessions));
      }
    } catch (error) {
      console.error("Failed to load sessions:", error);
    } finally {
      setLoadingSessions(false);
    }
  };

  const saveSessions = (newSessions: ProfileSession[]) => {
    localStorage.setItem('profileInterviewSessions', JSON.stringify(newSessions));
    setSessions(newSessions);
  };

  const startNewSession = async () => {
    try {
      setIsLoading(true);

      // Call the new Profile Interview API
      const res = await api.startProfileInterview({ language: "zh" });

      if (!res.success || !res.data) {
        console.error("Failed to start profile interview:", res);
        return;
      }

      const data = res.data;

      // Create a new profile session
      const newSession: ProfileSession = {
        id: data.sessionId,
        sessionId: data.sessionId,
        status: "active",
        currentPhase: data.currentPhase,
        phaseName: data.currentPhase,
        createdAt: new Date().toISOString(),
      };

      const newSessions = [newSession, ...sessions];
      saveSessions(newSessions);
      setActiveSession(newSession);

      // Show welcome message and first question from AI
      const welcomeContent = data.welcomeMessage + "\n\n" + data.firstQuestion;

      setMessages([{
        id: 1,
        role: "ai",
        content: welcomeContent,
      }]);

      // Save messages to localStorage
      localStorage.setItem(`profileInterview_${data.sessionId}_messages`, JSON.stringify([{
        id: 1,
        role: "ai",
        content: welcomeContent,
      }]));

    } catch (error) {
      console.error("Failed to create session:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const selectSession = async (session: ProfileSession) => {
    setActiveSession(session);
    // Load messages from localStorage
    const savedMessages = localStorage.getItem(`profileInterview_${session.sessionId}_messages`);
    if (savedMessages) {
      setMessages(JSON.parse(savedMessages));
    } else {
      setMessages([]);
    }
  };

  const deleteSession = async (sessionId: string) => {
    try {
      const newSessions = sessions.filter(s => s.id !== sessionId);
      saveSessions(newSessions);
      localStorage.removeItem(`profileInterview_${sessionId}_messages`);

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

    const messageToSend = input.trim(); // Save the input value before clearing

    const userMsg: Message = {
      id: Date.now(),
      role: "user",
      content: messageToSend,
    };

    const newMessages = [...messages, userMsg];
    setMessages(newMessages);
    setInput("");

    // Reset textarea height
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }

    setIsLoading(true);

    try {
      // Call the Profile Interview API to send message
      const res = await api.sendProfileInterviewMessage(activeSession.sessionId, messageToSend);

      if (!res.success || !res.data) {
        console.error("Failed to send message:", res);
        setIsLoading(false);
        return;
      }

      const data = res.data;

      // Add AI response to messages
      const aiMsg: Message = {
        id: Date.now() + 1,
        role: "ai",
        content: data.response,
      };

      const updatedMessages = [...newMessages, aiMsg];
      setMessages(updatedMessages);

      // Save messages to localStorage
      localStorage.setItem(`profileInterview_${activeSession.sessionId}_messages`, JSON.stringify(updatedMessages));

      // Update session phase
      if (data.currentPhase !== activeSession.currentPhase) {
        const updatedSession = {
          ...activeSession,
          currentPhase: data.currentPhase,
          phaseName: data.phaseName,
          status: data.isComplete ? "completed" as const : "active" as const,
        };
        setActiveSession(updatedSession);

        const updatedSessions = sessions.map(s =>
          s.id === activeSession.id ? updatedSession : s
        );
        saveSessions(updatedSessions);
      }

    } catch (error) {
      console.error("Failed to send message:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    // Enter without Shift = Send
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
    // Shift+Enter = New line (default behavior, no action needed)
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInput(e.target.value);

    // Auto-resize logic
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = Math.min(textarea.scrollHeight, 200) + 'px';
    }
  };

  const finishInterview = async () => {
    if (!activeSession) return;

    try {
      setIsLoading(true);
      const res = await api.finishProfileInterview(activeSession.sessionId);

      if (res.success && res.data) {
        // Update session status
        const updatedSession = { ...activeSession, status: "completed" as const };
        setActiveSession(updatedSession);

        const updatedSessions = sessions.map(s =>
          s.id === activeSession.id ? updatedSession : s
        );
        saveSessions(updatedSessions);

        // Add completion message
        const completionMsg: Message = {
          id: Date.now(),
          role: "ai",
          content: `🎉 面试完成！\n\n完整度评分: ${res.data.completenessScore}%\n${res.data.syncedToProfile ? '✅ 已同步到个人资料' : ''}\n\n${res.data.missingSections.length > 0 ? `缺失部分: ${res.data.missingSections.join(', ')}` : ''}`,
        };

        const updatedMessages = [...messages, completionMsg];
        setMessages(updatedMessages);
        localStorage.setItem(`profileInterview_${activeSession.sessionId}_messages`, JSON.stringify(updatedMessages));
      }
    } catch (error) {
      console.error("Failed to finish interview:", error);
    } finally {
      setIsLoading(false);
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
          New Profile Interview
        </Button>

        <div className="flex-1 overflow-y-auto space-y-2">
          {sessions.length === 0 ? (
            <Card className="p-4 text-center text-muted-foreground text-sm">
              No sessions yet. Click the button above to start a deep profile interview.
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
                    <span className="font-medium text-sm">Profile Interview</span>
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
                  Phase: {session.currentPhase || "basic_info"}
                </p>
                <p className="text-xs text-muted-foreground">
                  {session.createdAt ? new Date(session.createdAt).toLocaleDateString() : ""}
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
              <p className="font-medium">Profile Interview (Multi-Agent)</p>
              <p className="text-sm mt-2">Start a new session to begin deep background collection</p>
              <p className="text-xs mt-1">Uses AI-powered Questioner, Analyzer & Summarizer</p>
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
                  <h2 className="font-semibold">Profile Interview AI</h2>
                  <p className="text-xs text-muted-foreground">
                    Phase: {activeSession.currentPhase || "basic_info"} | Multi-Agent Architecture
                  </p>
                </div>
              </div>
              <div className="flex gap-2">
                {activeSession.status === "active" && (
                  <Button variant="outline" size="sm" onClick={finishInterview} disabled={isLoading}>
                    <CheckCircle className="h-4 w-4 mr-1" /> Finish & Sync
                  </Button>
                )}
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
                  <div className="bg-muted/50 border rounded-2xl px-4 py-3">
                    <div className="flex items-center">
                      <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
                      <span className="ml-2 text-sm text-muted-foreground">AI is thinking...</span>
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* Input Area */}
            <div className="p-4 border-t bg-muted/10">
              <div className="flex gap-2 items-end">
                <Textarea
                  ref={textareaRef}
                  placeholder="Type your answer..."
                  value={input}
                  onChange={handleInputChange}
                  onKeyDown={handleKeyDown}
                  className="flex-1 bg-background min-h-[44px] max-h-[200px] py-3 resize-none overflow-y-auto"
                  disabled={isLoading || activeSession.status === "completed"}
                  rows={1}
                />
                <Button
                  onClick={handleSend}
                  disabled={isLoading || !input.trim() || activeSession.status === "completed"}
                  size="icon"
                  className="h-[44px] w-[44px]"
                >
                  <Send className="h-4 w-4" />
                </Button>
              </div>
              <p className="text-[10px] text-center text-muted-foreground mt-2">
                Press Enter to send, Shift+Enter for new line
              </p>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
