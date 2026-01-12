"use client";

import { useState, useRef, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import { Send, Bot, User, Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

type Message = {
  id: number;
  role: "ai" | "user";
  content: string;
};

const INITIAL_MESSAGE: Message = {
  id: 1,
  role: "ai",
  content: "Hello! I\'m your CVibe AI Assistant. My goal is to help you build a comprehensive profile database. Let\'s start with your **Education**. \n\nCould you tell me which university you attended and what was your major?",
};

export default function InterviewPage() {
  const [messages, setMessages] = useState<Message[]>([INITIAL_MESSAGE]);
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom
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

    // Mock AI Response with delay
    setTimeout(() => {
      const aiMsg: Message = {
        id: Date.now() + 1,
        role: "ai",
        content: generateMockResponse(messages.length),
      };
      setMessages((prev) => [...prev, aiMsg]);
      setIsLoading(false);
    }, 1500);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="h-full flex flex-col w-full p-4">
      <div className="flex-1 overflow-hidden rounded-xl border bg-background shadow-sm flex flex-col h-full">
        {/* Header */}
        <div className="p-4 border-b flex items-center gap-3 bg-muted/20">
            <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                <Bot className="h-6 w-6 text-primary" />
            </div>
            <div>
                <h2 className="font-semibold">Profile Builder AI</h2>
                <p className="text-xs text-muted-foreground">Topic: Education & Experience</p>
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
              {/* Avatar */}
              <div className={cn(
                  "h-8 w-8 rounded-full flex items-center justify-center shrink-0",
                  msg.role === "ai" ? "bg-primary/10 text-primary" : "bg-muted text-muted-foreground"
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
                    placeholder="Type your answer here..."
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
            <p className="text-[10px] text-center text-muted-foreground mt-2">
                AI can make mistakes. Please verify important information.
            </p>
        </div>
      </div>
    </div>
  );
}

// Simple Mock Logic
function generateMockResponse(step: number): string {
    const responses = [
        "Got it. What was your **GPA** (Grade Point Average)?",
        "Impressive. Did you take any specific courses related to **Data Structures** or **Algorithms**?",
        "Okay. Now let\'s move on to your **Internships**. Have you completed any internships recently?",
        "What was your main responsibility during that internship?",
        "Did you use any specific technologies like **React**, **Python**, or **AWS**?",
        "Great details! I\'ve updated your profile database. Is there anything else you\'d like to add regarding your experience?",
    ];
    return responses[step % responses.length]; // Cycle through mock responses
}
