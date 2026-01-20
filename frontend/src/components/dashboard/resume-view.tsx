"use client";

import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  FileText, Clock, Eye, Upload, Loader2, Star, Trash2,
  RefreshCw, UserPlus, CheckCircle2, AlertCircle,
  Briefcase, GraduationCap, Code, FolderGit2, Award, Languages
} from "lucide-react";
import api, { Resume, ParsedContent } from "@/lib/api";
import { toast } from "sonner";

export function ResumeView() {
  const [resumes, setResumes] = useState<Resume[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedResume, setSelectedResume] = useState<Resume | null>(null);
  const [uploading, setUploading] = useState(false);
  const [deleting, setDeleting] = useState<string | null>(null);
  const [reparsing, setReparsing] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [showParsedContent, setShowParsedContent] = useState(false);

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
        toast.success("Resume uploaded and parsed successfully");
      }
    } catch (error) {
      console.error("Failed to upload resume:", error);
      toast.error("Failed to upload resume");
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
        toast.success("Primary resume updated");
      }
    } catch (error) {
      console.error("Failed to set primary:", error);
      toast.error("Failed to set primary resume");
    }
  };

  const deleteResume = async (resumeId: string) => {
    try {
      setDeleting(resumeId);
      const res = await api.deleteResume(resumeId);
      if (res.success) {
        setResumes(prev => prev.filter(r => r.id !== resumeId));
        if (selectedResume?.id === resumeId) {
          const remaining = resumes.filter(r => r.id !== resumeId);
          setSelectedResume(remaining[0] || null);
        }
        toast.success("Resume deleted");
      } else {
        toast.error("Failed to delete resume");
      }
    } catch (error) {
      console.error("Failed to delete resume:", error);
      toast.error("Failed to delete resume");
    } finally {
      setDeleting(null);
    }
  };

  const handleReparse = async () => {
    if (!selectedResume) return;
    
    try {
      setReparsing(true);
      const res = await api.reparseResume(selectedResume.id);
      if (res.success && res.data) {
        // Update the resume in list
        setResumes(prev => prev.map(r => r.id === selectedResume.id ? res.data! : r));
        setSelectedResume(res.data);
        toast.success("Resume re-parsed successfully");
      } else {
        toast.error(res.message || "Failed to re-parse resume");
      }
    } catch (error) {
      console.error("Failed to reparse resume:", error);
      toast.error("Failed to re-parse resume");
    } finally {
      setReparsing(false);
    }
  };

  const handleSyncToProfile = async () => {
    if (!selectedResume) return;
    
    try {
      setSyncing(true);
      const res = await api.syncResumeToProfile(selectedResume.id, {
        syncExperiences: true,
        syncEducations: true,
        syncSkills: true,
        syncProjects: true,
      });
      
      if (res.success && res.data) {
        const result = res.data;
        const parts = [];
        if (result.experiencesSynced) parts.push(`${result.experiencesSynced} experiences`);
        if (result.educationsSynced) parts.push(`${result.educationsSynced} educations`);
        if (result.skillsSynced) parts.push(`${result.skillsSynced} skills`);
        if (result.projectsSynced) parts.push(`${result.projectsSynced} projects`);
        
        toast.success(
          parts.length > 0 
            ? `Synced to profile: ${parts.join(', ')}` 
            : "Profile sync completed (no new items added)"
        );
      } else {
        toast.error(res.message || "Failed to sync to profile");
      }
    } catch (error) {
      console.error("Failed to sync to profile:", error);
      toast.error("Failed to sync resume to profile");
    } finally {
      setSyncing(false);
    }
  };

  const getStatusBadge = (status: Resume['status']) => {
    switch (status) {
      case 'COMPLETED':
        return <Badge variant="default" className="bg-green-500/20 text-green-600 border-green-500/30">Parsed</Badge>;
      case 'PROCESSING':
        return <Badge variant="secondary" className="bg-blue-500/20 text-blue-600 border-blue-500/30">Processing</Badge>;
      case 'FAILED':
        return <Badge variant="destructive">Failed</Badge>;
      default:
        return <Badge variant="outline">Pending</Badge>;
    }
  };

  const parsedContent = selectedResume?.parsedContent as ParsedContent | undefined;

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="grid gap-6 md:grid-cols-3 h-full animate-in fade-in slide-in-from-bottom-4 duration-500">
      {/* Resume List */}
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
                  selectedResume?.id === r.id ? "border-primary ring-1 ring-primary/20" : ""
                }`}
              >
                <div className="flex items-center gap-3">
                  <div className="bg-background p-2 rounded-md border">
                    <FileText className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="font-medium text-sm truncate max-w-[120px]">{r.originalName || r.fileName || `Resume ${r.id.slice(0, 8)}`}</p>
                    <p className="text-xs text-muted-foreground flex items-center gap-1">
                      <Clock className="h-3 w-3" /> {new Date(r.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                </div>
                <div className="flex flex-col items-end gap-1">
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
                        title="Set as primary"
                      >
                        <Star className="h-3 w-3" />
                      </Button>
                    )}
                    <Button 
                      size="icon" 
                      variant="ghost" 
                      className="h-6 w-6 hover:text-destructive"
                      onClick={(e) => { e.stopPropagation(); deleteResume(r.id); }}
                      disabled={deleting === r.id}
                      title="Delete resume"
                    >
                      {deleting === r.id ? (
                        <Loader2 className="h-3 w-3 animate-spin" />
                      ) : (
                        <Trash2 className="h-3 w-3" />
                      )}
                    </Button>
                  </div>
                  {getStatusBadge(r.status)}
                </div>
              </div>
            ))
          )}
          <div className="relative">
            <input
              type="file"
              accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
              onChange={handleUpload}
              className="absolute inset-0 opacity-0 cursor-pointer"
              disabled={uploading}
            />
            <Button className="w-full mt-2" variant="outline" disabled={uploading}>
              {uploading ? (
                <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Uploading & Parsing...</>
              ) : (
                <><Upload className="h-4 w-4 mr-2" /> Upload New Resume</>
              )}
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Preview and Actions */}
      <Card className="md:col-span-2 min-h-[500px] flex flex-col">
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <div>
            <CardTitle className="text-lg">
              {selectedResume?.originalName || selectedResume?.fileName || "Not Selected"}
            </CardTitle>
            {selectedResume && (
              <p className="text-sm text-muted-foreground mt-1">
                {selectedResume.status === 'COMPLETED' ? (
                  <span className="flex items-center gap-1 text-green-600">
                    <CheckCircle2 className="h-3 w-3" /> AI Parsed
                  </span>
                ) : selectedResume.status === 'FAILED' ? (
                  <span className="flex items-center gap-1 text-destructive">
                    <AlertCircle className="h-3 w-3" /> Parse Failed: {selectedResume.errorMessage}
                  </span>
                ) : (
                  <span className="flex items-center gap-1 text-muted-foreground">
                    <Loader2 className="h-3 w-3 animate-spin" /> Processing...
                  </span>
                )}
              </p>
            )}
          </div>
          <div className="flex items-center gap-2">
            {selectedResume?.status === 'COMPLETED' && (
              <>
                <Button 
                  size="sm" 
                  variant="outline"
                  onClick={() => setShowParsedContent(!showParsedContent)}
                >
                  {showParsedContent ? 'Show PDF' : 'Show Parsed Data'}
                </Button>
                <Button 
                  size="sm" 
                  variant="default"
                  onClick={handleSyncToProfile}
                  disabled={syncing}
                >
                  {syncing ? (
                    <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Syncing...</>
                  ) : (
                    <><UserPlus className="h-4 w-4 mr-2" /> Sync to Profile</>
                  )}
                </Button>
              </>
            )}
            {(selectedResume?.status === 'FAILED' || selectedResume?.status === 'COMPLETED') && (
              <Button 
                size="sm" 
                variant="outline"
                onClick={handleReparse}
                disabled={reparsing}
              >
                {reparsing ? (
                  <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Reparsing...</>
                ) : (
                  <><RefreshCw className="h-4 w-4 mr-2" /> {selectedResume?.status === 'FAILED' ? 'Retry Parse' : 'Reparse'}</>
                )}
              </Button>
            )}
            {(selectedResume?.downloadUrl || selectedResume?.fileUrl) && (
              <Button 
                size="sm" 
                variant="ghost"
                onClick={() => window.open(selectedResume.downloadUrl || selectedResume.fileUrl, "_blank")}
              >
                <Eye className="h-4 w-4 mr-2" /> Full Screen
              </Button>
            )}
          </div>
        </CardHeader>
        
        <CardContent className="flex-1 overflow-auto">
          {showParsedContent && parsedContent ? (
            <div className="space-y-6 p-4 bg-muted/20 rounded-xl border">
              {/* Personal Info */}
              {parsedContent.personalInfo && (
                <div>
                  <h3 className="font-semibold text-lg mb-2">Personal Information</h3>
                  <div className="grid grid-cols-2 gap-2 text-sm">
                    {parsedContent.personalInfo.name && <div><span className="text-muted-foreground">Name:</span> {parsedContent.personalInfo.name}</div>}
                    {parsedContent.personalInfo.email && <div><span className="text-muted-foreground">Email:</span> {parsedContent.personalInfo.email}</div>}
                    {parsedContent.personalInfo.phone && <div><span className="text-muted-foreground">Phone:</span> {parsedContent.personalInfo.phone}</div>}
                    {parsedContent.personalInfo.location && <div><span className="text-muted-foreground">Location:</span> {parsedContent.personalInfo.location}</div>}
                    {parsedContent.personalInfo.linkedin && <div><span className="text-muted-foreground">LinkedIn:</span> <a href={parsedContent.personalInfo.linkedin} target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">View Profile</a></div>}
                    {parsedContent.personalInfo.github && <div><span className="text-muted-foreground">GitHub:</span> <a href={parsedContent.personalInfo.github} target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">View Profile</a></div>}
                  </div>
                </div>
              )}

              {/* Summary */}
              {parsedContent.summary && (
                <div>
                  <h3 className="font-semibold text-lg mb-2">Summary</h3>
                  <p className="text-sm text-muted-foreground">{parsedContent.summary}</p>
                </div>
              )}

              {/* Experience */}
              {parsedContent.experiences && parsedContent.experiences.length > 0 && (
                <div>
                  <h3 className="font-semibold text-lg mb-2 flex items-center gap-2">
                    <Briefcase className="h-4 w-4" /> Work Experience ({parsedContent.experiences.length})
                  </h3>
                  <div className="space-y-3">
                    {parsedContent.experiences.map((exp, i) => (
                      <div key={i} className="p-3 bg-background rounded-lg border">
                        <div className="font-medium">{exp.title} at {exp.company}</div>
                        <div className="text-xs text-muted-foreground">{exp.startDate} - {exp.endDate || 'Present'} {exp.location && `| ${exp.location}`}</div>
                        {exp.description && <p className="text-sm mt-1">{exp.description}</p>}
                        {exp.technologies && exp.technologies.length > 0 && (
                          <div className="flex flex-wrap gap-1 mt-2">
                            {exp.technologies.map((tech, j) => (
                              <Badge key={j} variant="secondary" className="text-xs">{tech}</Badge>
                            ))}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Education */}
              {parsedContent.education && parsedContent.education.length > 0 && (
                <div>
                  <h3 className="font-semibold text-lg mb-2 flex items-center gap-2">
                    <GraduationCap className="h-4 w-4" /> Education ({parsedContent.education.length})
                  </h3>
                  <div className="space-y-3">
                    {parsedContent.education.map((edu, i) => (
                      <div key={i} className="p-3 bg-background rounded-lg border">
                        <div className="font-medium">{edu.degree} in {edu.field}</div>
                        <div className="text-sm">{edu.school}</div>
                        <div className="text-xs text-muted-foreground">{edu.startDate} - {edu.endDate} {edu.gpa && `| GPA: ${edu.gpa}`}</div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Projects */}
              {parsedContent.projects && parsedContent.projects.length > 0 && (
                <div>
                  <h3 className="font-semibold text-lg mb-2 flex items-center gap-2">
                    <FolderGit2 className="h-4 w-4" /> Projects ({parsedContent.projects.length})
                  </h3>
                  <div className="space-y-3">
                    {parsedContent.projects.map((proj, i) => (
                      <div key={i} className="p-3 bg-background rounded-lg border">
                        <div className="font-medium">{proj.name}</div>
                        {proj.description && <p className="text-sm text-muted-foreground mt-1">{proj.description}</p>}
                        {proj.technologies && proj.technologies.length > 0 && (
                          <div className="flex flex-wrap gap-1 mt-2">
                            {proj.technologies.map((tech, j) => (
                              <Badge key={j} variant="outline" className="text-xs">{tech}</Badge>
                            ))}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Skills */}
              {parsedContent.skills && parsedContent.skills.length > 0 && (
                <div>
                  <h3 className="font-semibold text-lg mb-2 flex items-center gap-2">
                    <Code className="h-4 w-4" /> Skills ({parsedContent.skills.length})
                  </h3>
                  <div className="flex flex-wrap gap-2">
                    {parsedContent.skills.map((skill, i) => (
                      <Badge key={i} variant="secondary">
                        {skill.name} {skill.level && <span className="text-xs opacity-70 ml-1">({skill.level})</span>}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}

              {/* Certifications */}
              {parsedContent.certifications && parsedContent.certifications.length > 0 && (
                <div>
                  <h3 className="font-semibold text-lg mb-2 flex items-center gap-2">
                    <Award className="h-4 w-4" /> Certifications ({parsedContent.certifications.length})
                  </h3>
                  <div className="space-y-2">
                    {parsedContent.certifications.map((cert, i) => (
                      <div key={i} className="p-3 bg-background rounded-lg border">
                        <div className="font-medium">{cert.name}</div>
                        {cert.issuer && <div className="text-sm text-muted-foreground">{cert.issuer}</div>}
                        {cert.date && <div className="text-xs text-muted-foreground">{cert.date}</div>}
                        {cert.url && (
                          <a href={cert.url} target="_blank" rel="noopener noreferrer" className="text-xs text-primary hover:underline">
                            View Certificate
                          </a>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Languages */}
              {parsedContent.languages && parsedContent.languages.length > 0 && (
                <div>
                  <h3 className="font-semibold text-lg mb-2 flex items-center gap-2">
                    <Languages className="h-4 w-4" /> Languages ({parsedContent.languages.length})
                  </h3>
                  <div className="flex flex-wrap gap-2">
                    {parsedContent.languages.map((lang, i) => (
                      <Badge key={i} variant="outline">
                        {lang.language} {lang.proficiency && <span className="text-xs opacity-70 ml-1">({lang.proficiency})</span>}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="h-full min-h-[400px] bg-muted/20 rounded-xl border-2 border-dashed border-muted-foreground/20 flex items-center justify-center">
              {(selectedResume?.downloadUrl || selectedResume?.fileUrl) ? (
                <iframe
                  src={selectedResume.downloadUrl || selectedResume.fileUrl}
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
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
