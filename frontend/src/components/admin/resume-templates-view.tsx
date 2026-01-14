"use client";

import { useState } from "react";
import { Plus, Trash2, Edit, FileText, LayoutTemplate } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";

// Mock Data
const INITIAL_TEMPLATES = [
  { id: 1, name: "Modern Tech", category: "Technology", downloads: 1240, status: "Active" },
  { id: 2, name: "Classic Corporate", category: "Business", downloads: 850, status: "Active" },
  { id: 3, name: "Creative Designer", category: "Design", downloads: 560, status: "Active" },
  { id: 4, name: "Academic Simple", category: "Education", downloads: 320, status: "Draft" },
];

export function ResumeTemplatesView() {
  const [templates, setTemplates] = useState(INITIAL_TEMPLATES);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [newTemplateName, setNewTemplateName] = useState("");
  const [newTemplateCategory, setNewTemplateCategory] = useState("");

  const handleAddTemplate = () => {
    if (!newTemplateName) return;
    const newTemplate = {
      id: templates.length + 1,
      name: newTemplateName,
      category: newTemplateCategory || "General",
      downloads: 0,
      status: "Draft",
    };
    setTemplates([...templates, newTemplate]);
    setNewTemplateName("");
    setNewTemplateCategory("");
    setIsDialogOpen(false);
  };

  const handleDelete = (id: number) => {
    setTemplates(templates.filter(t => t.id !== id));
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-medium">Resume Templates</h3>
          <p className="text-sm text-muted-foreground">Manage the available resume templates for users.</p>
        </div>
        <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
          <DialogTrigger asChild>
            <Button>
              <Plus className="mr-2 h-4 w-4" /> Add Template
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Add New Template</DialogTitle>
              <DialogDescription>
                Create a new resume template entry.
              </DialogDescription>
            </DialogHeader>
            <div className="grid gap-4 py-4">
              <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="name" className="text-right">
                  Name
                </Label>
                <Input
                  id="name"
                  value={newTemplateName}
                  onChange={(e) => setNewTemplateName(e.target.value)}
                  className="col-span-3"
                  placeholder="e.g., Professional Minimalist"
                />
              </div>
              <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="category" className="text-right">
                  Category
                </Label>
                <Input
                  id="category"
                  value={newTemplateCategory}
                  onChange={(e) => setNewTemplateCategory(e.target.value)}
                  className="col-span-3"
                  placeholder="e.g., Engineering"
                />
              </div>
            </div>
            <DialogFooter>
              <Button onClick={handleAddTemplate}>Save Template</Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {templates.map((template) => (
          <Card key={template.id} className="overflow-hidden group">
            <div className="aspect-[3/4] bg-muted relative flex items-center justify-center border-b">
               {/* Placeholder for Template Preview */}
               <LayoutTemplate className="h-16 w-16 text-muted-foreground/50" />
               <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                  <Button variant="secondary" size="sm">
                      <Edit className="h-4 w-4 mr-1" /> Edit
                  </Button>
               </div>
            </div>
            <CardHeader className="p-4 pb-2">
              <div className="flex justify-between items-start">
                  <CardTitle className="text-base">{template.name}</CardTitle>
                  <Badge variant={template.status === "Active" ? "default" : "secondary"}>
                      {template.status}
                  </Badge>
              </div>
            </CardHeader>
            <CardContent className="p-4 pt-0">
               <p className="text-xs text-muted-foreground">{template.category}</p>
               <div className="flex items-center text-xs text-muted-foreground mt-2">
                   <FileText className="h-3 w-3 mr-1" /> {template.downloads} uses
               </div>
            </CardContent>
            <CardFooter className="p-4 pt-0 flex justify-end">
                <Button variant="ghost" size="icon" className="h-8 w-8 text-destructive hover:text-destructive/90" onClick={() => handleDelete(template.id)}>
                    <Trash2 className="h-4 w-4" />
                </Button>
            </CardFooter>
          </Card>
        ))}
      </div>
    </div>
  );
}
