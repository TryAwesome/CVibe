"use client";

import { useState } from "react";
import { Plus, Building2, HelpCircle, Upload, Save } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

// Mock Data Structure
type Question = {
  id: string;
  text: string;
  difficulty: "Easy" | "Medium" | "Hard";
  topic: string;
};

type CompanyBank = {
  id: string;
  name: string;
  questions: Question[];
};

const INITIAL_BANKS: CompanyBank[] = [
  {
    id: "google",
    name: "Google",
    questions: [
      { id: "1", text: "Invert a Binary Tree", difficulty: "Easy", topic: "Algorithms" },
      { id: "2", text: "Design a URL Shortener", difficulty: "Hard", topic: "System Design" },
      { id: "3", text: "Tell me about a time you failed.", difficulty: "Medium", topic: "Behavioral" },
    ]
  },
  {
    id: "amazon",
    name: "Amazon",
    questions: [
      { id: "4", text: "Explain Amazon's Leadership Principles.", difficulty: "Medium", topic: "Behavioral" },
      { id: "5", text: "Design a Parking Lot system.", difficulty: "Medium", topic: "OOD" },
    ]
  }
];

export function InterviewQuestionsView() {
  const [banks, setBanks] = useState<CompanyBank[]>(INITIAL_BANKS);
  const [selectedCompanyId, setSelectedCompanyId] = useState<string>(INITIAL_BANKS[0].id);
  const [newQuestionText, setNewQuestionText] = useState("");
  const [newCompany, setNewCompany] = useState("");

  const selectedBank = banks.find(b => b.id === selectedCompanyId);

  const handleAddQuestion = () => {
    if (!newQuestionText || !selectedBank) return;
    
    const newQuestion: Question = {
      id: Math.random().toString(36).substr(2, 9),
      text: newQuestionText,
      difficulty: "Medium", // Default for now
      topic: "General"
    };

    const updatedBanks = banks.map(b => {
      if (b.id === selectedCompanyId) {
        return { ...b, questions: [...b.questions, newQuestion] };
      }
      return b;
    });

    setBanks(updatedBanks);
    setNewQuestionText("");
  };

  const handleAddCompany = () => {
      if(!newCompany) return;
      const newBank: CompanyBank = {
          id: newCompany.toLowerCase().replace(/\s+/g, '-'),
          name: newCompany,
          questions: []
      };
      setBanks([...banks, newBank]);
      setSelectedCompanyId(newBank.id);
      setNewCompany("");
  }

  return (
    <div className="flex h-[700px] gap-6">
      {/* Sidebar - Company List */}
      <Card className="w-1/3 flex flex-col">
        <CardHeader>
          <CardTitle className="text-lg">Companies</CardTitle>
          <CardDescription>Select a company to manage</CardDescription>
        </CardHeader>
        <CardContent className="flex-1 overflow-y-auto space-y-4">
            <div className="flex gap-2 mb-4">
                <Input 
                    placeholder="New Company..." 
                    value={newCompany}
                    onChange={(e) => setNewCompany(e.target.value)}
                />
                <Button size="icon" onClick={handleAddCompany}>
                    <Plus className="h-4 w-4" />
                </Button>
            </div>
            <div className="space-y-1">
                {banks.map((bank) => (
                    <button
                        key={bank.id}
                        onClick={() => setSelectedCompanyId(bank.id)}
                        className={`w-full flex items-center justify-between p-3 rounded-lg text-sm font-medium transition-colors ${selectedCompanyId === bank.id ? 'bg-primary/10 text-primary' : 'hover:bg-muted'}`}
                    >
                        <div className="flex items-center">
                            <Building2 className="mr-2 h-4 w-4 opacity-70" />
                            {bank.name}
                        </div>
                        <Badge variant="outline" className="text-xs">{bank.questions.length}</Badge>
                    </button>
                ))}
            </div>
        </CardContent>
      </Card>

      {/* Main Content - Questions */}
      <Card className="flex-1 flex flex-col">
        <CardHeader className="flex flex-row items-center justify-between border-b pb-4">
           <div>
               <CardTitle>{selectedBank?.name} Question Bank</CardTitle>
               <CardDescription>Manage interview questions for {selectedBank?.name}</CardDescription>
           </div>
           <Button variant="outline">
               <Upload className="mr-2 h-4 w-4" /> Bulk Import
           </Button>
        </CardHeader>
        <CardContent className="flex-1 overflow-hidden flex flex-col p-0">
            {/* Add New Question Area */}
            <div className="p-4 border-b bg-muted/20">
                <div className="flex gap-2">
                    <Textarea 
                        placeholder="Type a new question here..." 
                        className="min-h-[60px] resize-none"
                        value={newQuestionText}
                        onChange={(e) => setNewQuestionText(e.target.value)}
                    />
                    <Button className="h-auto" onClick={handleAddQuestion}>
                        Add
                    </Button>
                </div>
            </div>

            {/* Questions List */}
            <div className="flex-1 overflow-y-auto p-4 space-y-3">
                {selectedBank?.questions.length === 0 ? (
                    <div className="text-center py-10 text-muted-foreground">
                        <HelpCircle className="mx-auto h-10 w-10 opacity-20 mb-2" />
                        No questions yet. Add one above!
                    </div>
                ) : (
                    selectedBank?.questions.map((q) => (
                        <div key={q.id} className="p-3 border rounded-lg bg-card hover:bg-accent/50 transition-colors group">
                            <div className="flex justify-between items-start">
                                <p className="font-medium text-sm">{q.text}</p>
                                <div className="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                    {/* Mock actions */}
                                    <Button variant="ghost" size="icon" className="h-6 w-6"><Plus className="h-3 w-3" /></Button>
                                </div>
                            </div>
                            <div className="flex gap-2 mt-2">
                                <Badge variant="secondary" className="text-[10px]">{q.difficulty}</Badge>
                                <Badge variant="outline" className="text-[10px]">{q.topic}</Badge>
                            </div>
                        </div>
                    ))
                )}
            </div>
        </CardContent>
      </Card>
    </div>
  );
}
