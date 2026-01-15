"use client";

import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { FileText, Clock, Eye, Upload, Loader2, Star } from "lucide-react";
import api, { Resume } from "@/lib/api";

export function ResumeView() {
  const [resumes, setResumes] = useState<Resume[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedResume, setSelectedResume] = useState<Resume | null>(null);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    loadResumes();
  }, []);

  const loadResumes = async () => {
    try {
      setLoading(true);
      const res = await api.getResumes();
      if (res.success && res.data) {
        setResumes(res.data);
        const primary = res.data.find(r => r.isPrimary);
        setSelectedResume(primary || res.data[0] || null);
      }
    } catch (error) {
      console.error("Failed to load resumes:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setUploading(true);
      const res = await api.uploadResume(file);
      if (res.success && res.data) {
        setResumes(prev => [res.data!, ...prev]);
        setSelectedResume(res.data);
      }
    } catch (error) {
      console.error("Failed to upload resume:", error);
    } finally {
      setUploading(false);
    }
  };

  const setPrimary = async (resumeId: string) => {
    try {
      const res = await api.setPrimaryResume(resumeId);
      if (res.success) {
        setResumes(prev => prev.map(r => ({
          ...r,
          isPrimary: r.id === resumeId
        })));
      }
    } catch (error) {
      console.error("Failed to set primary:", error);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="grid gap-6 md:grid-cols-3 h-full animate-in fade-in slide-in-from-bottom-4 duration-500">
      {/* Version History List */}
      <Card className="md:col-span-1 h-fit">
        <CardHeader>
          <CardTitle className="text-lg">Resume List</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4">
          {resumes.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-4">No resumes yet. Please upload one.</p>
          ) : (
            resumes.map((r) => (
              <div
                key={r.id}
                onClick={() => setSelectedResume(r)}
                className={`flex items-center justify-between p-3 rounded-lg border bg-muted/50 hover:bg-muted transition-colors cursor-pointer ${
                  selectedResume?.id === r.id ? "border-primary" : ""
                }`}
              >
                <div className="flex items-center gap-3">
                  <div className="bg-background p-2 rounded-md border">
                    <FileText className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="font-medium text-sm">{r.fileName || `Resume ${r.id}`}</p>
                    <p className="text-xs text-muted-foreground flex items-center gap-1">
                      <Clock className="h-3 w-3" /> {new Date(r.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-1">
                  {r.isPrimary && (
                    <Badge variant="default" className="text-[10px]">Primary</Badge>
                  )}
                  {!r.isPrimary && (
                    <Button 
                      size="icon" 
                      variant="ghost" 
                      className="h-6 w-6"
                      onClick={(e) => { e.stopPropagation(); setPrimary(r.id); }}
                    >
                      <Star className="h-3 w-3" />
                    </Button>
                  )}
                </div>
              </div>
            ))
          )}
          <div className="relative">
            <input
              type="file"
              accept=".pdf,.doc,.docx"
              onChange={handleUpload}
              className="absolute inset-0 opacity-0 cursor-pointer"
              disabled={uploading}
            />
            <Button className="w-full mt-2" variant="outline" disabled={uploading}>
              {uploading ? (
                <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Uploading...</>
              ) : (
                <><Upload className="h-4 w-4 mr-2" /> Upload New Resume</>
              )}
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* PDF Preview Placeholder */}
      <Card className="md:col-span-2 min-h-[500px] flex flex-col">
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-lg">
            Preview: {selectedResume?.fileName || "Not Selected"}
          </CardTitle>
          {selectedResume?.fileUrl && (
            <Button 
              size="sm" 
              variant="ghost"
              onClick={() => window.open(selectedResume.fileUrl, "_blank")}
            >
              <Eye className="h-4 w-4 mr-2" /> Full Screen
            </Button>
          )}
        </CardHeader>
        <CardContent className="flex-1 bg-muted/20 m-6 rounded-xl border-2 border-dashed border-muted-foreground/20 flex items-center justify-center">
          {selectedResume?.fileUrl ? (
            <iframe
              src={selectedResume.fileUrl}
              className="w-full h-full min-h-[400px] rounded-lg"
              title="Resume Preview"
            />
          ) : (
            <div className="text-center text-muted-foreground">
              <FileText className="h-16 w-16 mx-auto mb-4 opacity-50" />
              <p>PDF Preview</p>
              <p className="text-sm opacity-60">Select a resume to view</p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
