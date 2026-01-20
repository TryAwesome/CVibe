"use client";

import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Plus, Pencil, Briefcase, Loader2, Trash2, X, Check, Code, GraduationCap, FolderGit2, Award, Languages } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { toast } from "sonner";
import api, { Profile, Experience, Skill, Education, Project, AddExperienceRequest, AddEducationRequest, AddProjectRequest, Language, AddLanguageRequest, Certification, AddCertificationRequest } from "@/lib/api";

export function ProfileView() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [experiences, setExperiences] = useState<Experience[]>([]);
  const [educations, setEducations] = useState<Education[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [skills, setSkills] = useState<Skill[]>([]);
  const [languages, setLanguages] = useState<Language[]>([]);
  const [certifications, setCertifications] = useState<Certification[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingExperience, setEditingExperience] = useState<Experience | null>(null);
  const [editingEducation, setEditingEducation] = useState<Education | null>(null);
  const [editingProject, setEditingProject] = useState<Project | null>(null);
  const [editingLanguage, setEditingLanguage] = useState<Language | null>(null);
  const [editingCertification, setEditingCertification] = useState<Certification | null>(null);
  const [newSkill, setNewSkill] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [profileRes, experiencesRes, educationsRes, projectsRes, skillsRes, languagesRes, certificationsRes] = await Promise.all([
        api.getProfile(),
        api.getExperiences(),
        api.getEducations(),
        api.getProjects(),
        api.getSkills(),
        api.getLanguages(),
        api.getCertifications(),
      ]);
      if (profileRes.success && profileRes.data) setProfile(profileRes.data);
      if (experiencesRes.success && experiencesRes.data) setExperiences(experiencesRes.data);
      if (educationsRes.success && educationsRes.data) setEducations(educationsRes.data);
      if (projectsRes.success && projectsRes.data) setProjects(projectsRes.data);
      if (skillsRes.success && skillsRes.data) setSkills(skillsRes.data);
      if (languagesRes.success && languagesRes.data) setLanguages(languagesRes.data);
      if (certificationsRes.success && certificationsRes.data) setCertifications(certificationsRes.data);
    } catch (error) {
      console.error("Failed to load profile data:", error);
      toast.error("Failed to load data");
    } finally {
      setLoading(false);
    }
  };

  // ==================== Experience Methods ====================
  const addExperience = async () => {
    try {
      setSaving(true);
      const today = new Date().toISOString().split('T')[0];
      const request: AddExperienceRequest = {
        company: "New Company",
        title: "New Position",
        startDate: today,
        employmentType: "FULL_TIME",
        isCurrent: true,
      };
      const res = await api.createExperience(request);
      if (res.success && res.data) {
        setExperiences(prev => [...prev, res.data!]);
        setEditingExperience(res.data);
        toast.success("Experience added");
      } else {
        toast.error(res.error || "Failed to add");
      }
    } catch (error) {
      console.error("Failed to add experience:", error);
      toast.error("Failed to add experience");
    } finally {
      setSaving(false);
    }
  };

  const updateExperience = async (exp: Experience) => {
    try {
      setSaving(true);
      const res = await api.updateExperience(String(exp.id), exp);
      if (res.success) {
        setExperiences(prev => prev.map(e => e.id === exp.id ? exp : e));
        setEditingExperience(null);
        toast.success("Experience updated");
      } else {
        toast.error(res.error || "Failed to update");
      }
    } catch (error) {
      console.error("Failed to update experience:", error);
      toast.error("Failed to update experience");
    } finally {
      setSaving(false);
    }
  };

  const deleteExperience = async (id: string) => {
    try {
      setSaving(true);
      const res = await api.deleteExperience(id);
      if (res.success) {
        setExperiences(prev => prev.filter(e => e.id !== id));
        toast.success("Experience deleted");
      } else {
        toast.error(res.error || "Failed to delete");
      }
    } catch (error) {
      console.error("Failed to delete experience:", error);
      toast.error("Failed to delete experience");
    } finally {
      setSaving(false);
    }
  };

  // ==================== Education Methods ====================
  const addEducation = async () => {
    try {
      setSaving(true);
      const request: AddEducationRequest = {
        school: "New School",
        degree: "Bachelor's",
        isCurrent: true,
      };
      const res = await api.createEducation(request);
      if (res.success && res.data) {
        setEducations(prev => [...prev, res.data!]);
        setEditingEducation(res.data);
        toast.success("Education added");
      } else {
        toast.error(res.error || "Failed to add");
      }
    } catch (error) {
      console.error("Failed to add education:", error);
      toast.error("Failed to add education");
    } finally {
      setSaving(false);
    }
  };

  const updateEducation = async (edu: Education) => {
    try {
      setSaving(true);
      const res = await api.updateEducation(String(edu.id), edu);
      if (res.success) {
        setEducations(prev => prev.map(e => e.id === edu.id ? edu : e));
        setEditingEducation(null);
        toast.success("Education updated");
      } else {
        toast.error(res.error || "Failed to update");
      }
    } catch (error) {
      console.error("Failed to update education:", error);
      toast.error("Failed to update education");
    } finally {
      setSaving(false);
    }
  };

  const deleteEducation = async (id: string) => {
    try {
      setSaving(true);
      const res = await api.deleteEducation(id);
      if (res.success) {
        setEducations(prev => prev.filter(e => e.id !== id));
        toast.success("Education deleted");
      } else {
        toast.error(res.error || "Failed to delete");
      }
    } catch (error) {
      console.error("Failed to delete education:", error);
      toast.error("Failed to delete education");
    } finally {
      setSaving(false);
    }
  };

  // ==================== Project Methods ====================
  const addProject = async () => {
    try {
      setSaving(true);
      const request: AddProjectRequest = {
        name: "New Project",
        isCurrent: false,
      };
      const res = await api.createProject(request);
      if (res.success && res.data) {
        setProjects(prev => [...prev, res.data!]);
        setEditingProject(res.data);
        toast.success("Project added");
      } else {
        toast.error(res.error || "Failed to add");
      }
    } catch (error) {
      console.error("Failed to add project:", error);
      toast.error("Failed to add project");
    } finally {
      setSaving(false);
    }
  };

  const updateProject = async (proj: Project) => {
    try {
      setSaving(true);
      const res = await api.updateProject(String(proj.id), proj);
      if (res.success) {
        setProjects(prev => prev.map(p => p.id === proj.id ? proj : p));
        setEditingProject(null);
        toast.success("Project updated");
      } else {
        toast.error(res.error || "Failed to update");
      }
    } catch (error) {
      console.error("Failed to update project:", error);
      toast.error("Failed to update project");
    } finally {
      setSaving(false);
    }
  };

  const deleteProject = async (id: string) => {
    try {
      setSaving(true);
      const res = await api.deleteProject(id);
      if (res.success) {
        setProjects(prev => prev.filter(p => p.id !== id));
        toast.success("Project deleted");
      } else {
        toast.error(res.error || "Failed to delete");
      }
    } catch (error) {
      console.error("Failed to delete project:", error);
      toast.error("Failed to delete project");
    } finally {
      setSaving(false);
    }
  };

  // ==================== Skill Methods ====================
  const addSkill = async () => {
    if (!newSkill.trim()) return;
    try {
      setSaving(true);
      const res = await api.createSkill({ name: newSkill, level: "intermediate" });
      if (res.success && res.data) {
        setSkills(prev => [...prev, res.data!]);
        setNewSkill("");
        toast.success("Skill added");
      } else {
        toast.error(res.error || "Failed to add skill");
      }
    } catch (error) {
      console.error("Failed to add skill:", error);
      toast.error("Failed to add skill");
    } finally {
      setSaving(false);
    }
  };

  const deleteSkill = async (id: string) => {
    try {
      setSaving(true);
      const res = await api.deleteSkill(id);
      if (res.success) {
        setSkills(prev => prev.filter(s => s.id !== id));
        toast.success("Skill deleted");
      } else {
        toast.error(res.error || "Failed to delete skill");
      }
    } catch (error) {
      console.error("Failed to delete skill:", error);
      toast.error("Failed to delete skill");
    } finally {
      setSaving(false);
    }
  };

  // ==================== Language Methods ====================
  const addLanguage = async () => {
    try {
      setSaving(true);
      const request: AddLanguageRequest = {
        language: "New Language",
        proficiency: "Conversational",
      };
      const res = await api.createLanguage(request);
      if (res.success && res.data) {
        setLanguages(prev => [...prev, res.data!]);
        setEditingLanguage(res.data);
        toast.success("Language added");
      } else {
        toast.error(res.error || "Failed to add");
      }
    } catch (error) {
      console.error("Failed to add language:", error);
      toast.error("Failed to add language");
    } finally {
      setSaving(false);
    }
  };

  const updateLanguage = async (lang: Language) => {
    try {
      setSaving(true);
      const res = await api.updateLanguage(String(lang.id), lang);
      if (res.success) {
        setLanguages(prev => prev.map(l => l.id === lang.id ? lang : l));
        setEditingLanguage(null);
        toast.success("Language updated");
      } else {
        toast.error(res.error || "Failed to update");
      }
    } catch (error) {
      console.error("Failed to update language:", error);
      toast.error("Failed to update language");
    } finally {
      setSaving(false);
    }
  };

  const deleteLanguage = async (id: string) => {
    try {
      setSaving(true);
      const res = await api.deleteLanguage(id);
      if (res.success) {
        setLanguages(prev => prev.filter(l => l.id !== id));
        toast.success("Language deleted");
      } else {
        toast.error(res.error || "Failed to delete");
      }
    } catch (error) {
      console.error("Failed to delete language:", error);
      toast.error("Failed to delete language");
    } finally {
      setSaving(false);
    }
  };

  // ==================== Certification Methods ====================
  const addCertification = async () => {
    try {
      setSaving(true);
      const request: AddCertificationRequest = {
        name: "New Certification",
        issuer: "Issuing Organization",
      };
      const res = await api.createCertification(request);
      if (res.success && res.data) {
        setCertifications(prev => [...prev, res.data!]);
        setEditingCertification(res.data);
        toast.success("Certification added");
      } else {
        toast.error(res.error || "Failed to add");
      }
    } catch (error) {
      console.error("Failed to add certification:", error);
      toast.error("Failed to add certification");
    } finally {
      setSaving(false);
    }
  };

  const updateCertification = async (cert: Certification) => {
    try {
      setSaving(true);
      const res = await api.updateCertification(String(cert.id), cert);
      if (res.success) {
        setCertifications(prev => prev.map(c => c.id === cert.id ? cert : c));
        setEditingCertification(null);
        toast.success("Certification updated");
      } else {
        toast.error(res.error || "Failed to update");
      }
    } catch (error) {
      console.error("Failed to update certification:", error);
      toast.error("Failed to update certification");
    } finally {
      setSaving(false);
    }
  };

  const deleteCertification = async (id: string) => {
    try {
      setSaving(true);
      const res = await api.deleteCertification(id);
      if (res.success) {
        setCertifications(prev => prev.filter(c => c.id !== id));
        toast.success("Certification deleted");
      } else {
        toast.error(res.error || "Failed to delete");
      }
    } catch (error) {
      console.error("Failed to delete certification:", error);
      toast.error("Failed to delete certification");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  // ==================== Experience Item Component ====================
  const ExperienceItem = ({ exp }: { exp: Experience }) => {
    if (editingExperience?.id === exp.id) {
      return (
        <div className="p-4 rounded-lg border bg-background space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-muted-foreground">Job Title</label>
              <Input
                placeholder="Title / Position"
                value={editingExperience.title}
                onChange={(e) => setEditingExperience({ ...editingExperience, title: e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground">Company Name</label>
              <Input
                placeholder="Company Name"
                value={editingExperience.company || ""}
                onChange={(e) => setEditingExperience({ ...editingExperience, company: e.target.value })}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-muted-foreground">Location</label>
              <Input
                placeholder="Location"
                value={editingExperience.location || ""}
                onChange={(e) => setEditingExperience({ ...editingExperience, location: e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground">Employment Type</label>
              <Select
                value={editingExperience.employmentType || "FULL_TIME"}
                onValueChange={(v) => setEditingExperience({ ...editingExperience, employmentType: v as Experience['employmentType'] })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="FULL_TIME">Full Time</SelectItem>
                  <SelectItem value="PART_TIME">Part Time</SelectItem>
                  <SelectItem value="CONTRACT">Contract</SelectItem>
                  <SelectItem value="INTERNSHIP">Internship</SelectItem>
                  <SelectItem value="FREELANCE">Freelance</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-muted-foreground">Start Date</label>
              <Input
                type="date"
                value={editingExperience.startDate || ""}
                onChange={(e) => setEditingExperience({ ...editingExperience, startDate: e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground">End Date</label>
              <Input
                type="date"
                value={editingExperience.endDate || ""}
                onChange={(e) => setEditingExperience({ ...editingExperience, endDate: e.target.value })}
                disabled={editingExperience.isCurrent}
              />
            </div>
          </div>
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={editingExperience.isCurrent || false}
              onChange={(e) => setEditingExperience({ ...editingExperience, isCurrent: e.target.checked, endDate: e.target.checked ? undefined : editingExperience.endDate })}
            />
            Currently Working Here
          </label>
          <div>
            <label className="text-xs text-muted-foreground">Description</label>
            <textarea
              placeholder="Description"
              className="w-full p-2 rounded border text-sm min-h-[80px]"
              value={editingExperience.description || ""}
              onChange={(e) => setEditingExperience({ ...editingExperience, description: e.target.value })}
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button size="sm" variant="ghost" onClick={() => setEditingExperience(null)} disabled={saving}>
              <X className="h-4 w-4 mr-1" /> Cancel
            </Button>
            <Button size="sm" onClick={() => updateExperience(editingExperience)} disabled={saving}>
              {saving ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : <Check className="h-4 w-4 mr-1" />} Save
            </Button>
          </div>
        </div>
      );
    }

    const typeLabel = {
      FULL_TIME: "Full Time",
      PART_TIME: "Part Time",
      CONTRACT: "Contract",
      INTERNSHIP: "Internship",
      FREELANCE: "Freelance",
    }[exp.employmentType || "FULL_TIME"];

    return (
      <div className="p-4 rounded-lg border bg-muted/20 flex justify-between items-start">
        <div>
          <h4 className="font-semibold">{exp.title || "Untitled"}</h4>
          {exp.company && (
            <p className="text-sm text-muted-foreground">
              {exp.company} {exp.location && `· ${exp.location}`}
            </p>
          )}
          {exp.employmentType && (
            <Badge variant="outline" className="text-xs mt-1">{typeLabel}</Badge>
          )}
          {(exp.startDate || exp.endDate) && (
            <p className="text-xs text-muted-foreground mt-1">
              {exp.startDate} - {exp.isCurrent ? "Present" : exp.endDate || ""}
            </p>
          )}
          {exp.description && (
            <p className="text-sm text-muted-foreground mt-2">{exp.description}</p>
          )}
        </div>
        <div className="flex gap-1">
          <Button size="icon" variant="ghost" className="h-8 w-8" onClick={() => setEditingExperience(exp)}>
            <Pencil className="h-3 w-3" />
          </Button>
          <Button size="icon" variant="ghost" className="h-8 w-8" onClick={() => deleteExperience(exp.id)}>
            <Trash2 className="h-3 w-3" />
          </Button>
        </div>
      </div>
    );
  };

  // ==================== Education Item Component ====================
  const EducationItem = ({ edu }: { edu: Education }) => {
    if (editingEducation?.id === edu.id) {
      return (
        <div className="p-4 rounded-lg border bg-background space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-muted-foreground">School</label>
              <Input
                placeholder="University / School Name"
                value={editingEducation.school}
                onChange={(e) => setEditingEducation({ ...editingEducation, school: e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground">Degree</label>
              <Input
                placeholder="e.g. Bachelor's, Master's"
                value={editingEducation.degree || ""}
                onChange={(e) => setEditingEducation({ ...editingEducation, degree: e.target.value })}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-muted-foreground">Field of Study</label>
              <Input
                placeholder="e.g. Computer Science"
                value={editingEducation.fieldOfStudy || ""}
                onChange={(e) => setEditingEducation({ ...editingEducation, fieldOfStudy: e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground">GPA</label>
              <Input
                placeholder="e.g. 3.8/4.0"
                value={editingEducation.gpa || ""}
                onChange={(e) => setEditingEducation({ ...editingEducation, gpa: e.target.value })}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-muted-foreground">Start Date</label>
              <Input
                type="date"
                value={editingEducation.startDate || ""}
                onChange={(e) => setEditingEducation({ ...editingEducation, startDate: e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground">End Date</label>
              <Input
                type="date"
                value={editingEducation.endDate || ""}
                onChange={(e) => setEditingEducation({ ...editingEducation, endDate: e.target.value })}
                disabled={editingEducation.isCurrent}
              />
            </div>
          </div>
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={editingEducation.isCurrent || false}
              onChange={(e) => setEditingEducation({ ...editingEducation, isCurrent: e.target.checked, endDate: e.target.checked ? undefined : editingEducation.endDate })}
            />
            Currently Studying Here
          </label>
          <div>
            <label className="text-xs text-muted-foreground">Description / Activities</label>
            <textarea
              placeholder="Relevant courses, clubs, achievements..."
              className="w-full p-2 rounded border text-sm min-h-[80px]"
              value={editingEducation.description || ""}
              onChange={(e) => setEditingEducation({ ...editingEducation, description: e.target.value })}
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button size="sm" variant="ghost" onClick={() => setEditingEducation(null)} disabled={saving}>
              <X className="h-4 w-4 mr-1" /> Cancel
            </Button>
            <Button size="sm" onClick={() => updateEducation(editingEducation)} disabled={saving}>
              {saving ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : <Check className="h-4 w-4 mr-1" />} Save
            </Button>
          </div>
        </div>
      );
    }

    return (
      <div className="p-4 rounded-lg border bg-muted/20 flex justify-between items-start">
        <div>
          <h4 className="font-semibold">{edu.school || "Untitled"}</h4>
          {(edu.degree || edu.fieldOfStudy) && (
            <p className="text-sm text-muted-foreground">
              {edu.degree}{edu.degree && edu.fieldOfStudy && " in "}{edu.fieldOfStudy}
            </p>
          )}
          {edu.gpa && (
            <Badge variant="outline" className="text-xs mt-1">GPA: {edu.gpa}</Badge>
          )}
          {(edu.startDate || edu.endDate) && (
            <p className="text-xs text-muted-foreground mt-1">
              {edu.startDate} - {edu.isCurrent ? "Present" : edu.endDate || ""}
            </p>
          )}
          {edu.description && (
            <p className="text-sm text-muted-foreground mt-2">{edu.description}</p>
          )}
        </div>
        <div className="flex gap-1">
          <Button size="icon" variant="ghost" className="h-8 w-8" onClick={() => setEditingEducation(edu)}>
            <Pencil className="h-3 w-3" />
          </Button>
          <Button size="icon" variant="ghost" className="h-8 w-8" onClick={() => deleteEducation(edu.id)}>
            <Trash2 className="h-3 w-3" />
          </Button>
        </div>
      </div>
    );
  };

  // ==================== Project Item Component ====================
  const ProjectItem = ({ proj }: { proj: Project }) => {
    if (editingProject?.id === proj.id) {
      return (
        <div className="p-4 rounded-lg border bg-background space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-muted-foreground">Project Name</label>
              <Input
                placeholder="Project Name"
                value={editingProject.name}
                onChange={(e) => setEditingProject({ ...editingProject, name: e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground">Project URL</label>
              <Input
                placeholder="https://example.com"
                value={editingProject.url || ""}
                onChange={(e) => setEditingProject({ ...editingProject, url: e.target.value })}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-muted-foreground">Repository URL</label>
              <Input
                placeholder="https://github.com/..."
                value={editingProject.repoUrl || ""}
                onChange={(e) => setEditingProject({ ...editingProject, repoUrl: e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground">Technologies (comma-separated)</label>
              <Input
                placeholder="React, Node.js, PostgreSQL"
                value={editingProject.technologies?.join(", ") || ""}
                onChange={(e) => setEditingProject({ ...editingProject, technologies: e.target.value.split(",").map(t => t.trim()).filter(t => t) })}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-muted-foreground">Start Date</label>
              <Input
                type="date"
                value={editingProject.startDate || ""}
                onChange={(e) => setEditingProject({ ...editingProject, startDate: e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground">End Date</label>
              <Input
                type="date"
                value={editingProject.endDate || ""}
                onChange={(e) => setEditingProject({ ...editingProject, endDate: e.target.value })}
                disabled={editingProject.isCurrent}
              />
            </div>
          </div>
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={editingProject.isCurrent || false}
              onChange={(e) => setEditingProject({ ...editingProject, isCurrent: e.target.checked, endDate: e.target.checked ? undefined : editingProject.endDate })}
            />
            Currently Working on This
          </label>
          <div>
            <label className="text-xs text-muted-foreground">Description</label>
            <textarea
              placeholder="Describe the project, your role, and key features..."
              className="w-full p-2 rounded border text-sm min-h-[80px]"
              value={editingProject.description || ""}
              onChange={(e) => setEditingProject({ ...editingProject, description: e.target.value })}
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button size="sm" variant="ghost" onClick={() => setEditingProject(null)} disabled={saving}>
              <X className="h-4 w-4 mr-1" /> Cancel
            </Button>
            <Button size="sm" onClick={() => updateProject(editingProject)} disabled={saving}>
              {saving ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : <Check className="h-4 w-4 mr-1" />} Save
            </Button>
          </div>
        </div>
      );
    }

    return (
      <div className="p-4 rounded-lg border bg-muted/20 flex justify-between items-start">
        <div>
          <h4 className="font-semibold">{proj.name || "Untitled"}</h4>
          {proj.description && (
            <p className="text-sm text-muted-foreground mt-1">{proj.description}</p>
          )}
          {proj.technologies && proj.technologies.length > 0 && (
            <div className="flex flex-wrap gap-1 mt-2">
              {proj.technologies.map((tech, idx) => (
                <Badge key={idx} variant="secondary" className="text-xs">{tech}</Badge>
              ))}
            </div>
          )}
          {(proj.startDate || proj.endDate) && (
            <p className="text-xs text-muted-foreground mt-1">
              {proj.startDate} - {proj.isCurrent ? "Present" : proj.endDate || ""}
            </p>
          )}
          {(proj.url || proj.repoUrl) && (
            <div className="flex gap-3 mt-2">
              {proj.url && (
                <a href={proj.url} target="_blank" rel="noopener noreferrer" className="text-xs text-primary hover:underline">
                  View Project
                </a>
              )}
              {proj.repoUrl && (
                <a href={proj.repoUrl} target="_blank" rel="noopener noreferrer" className="text-xs text-primary hover:underline">
                  View Code
                </a>
              )}
            </div>
          )}
        </div>
        <div className="flex gap-1">
          <Button size="icon" variant="ghost" className="h-8 w-8" onClick={() => setEditingProject(proj)}>
            <Pencil className="h-3 w-3" />
          </Button>
          <Button size="icon" variant="ghost" className="h-8 w-8" onClick={() => deleteProject(proj.id)}>
            <Trash2 className="h-3 w-3" />
          </Button>
        </div>
      </div>
    );
  };

  // ==================== Language Item Component ====================
  const LanguageItem = ({ lang }: { lang: Language }) => {
    if (editingLanguage?.id === lang.id) {
      return (
        <div className="p-4 rounded-lg border bg-background space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-muted-foreground">Language</label>
              <Input
                placeholder="e.g., English, Spanish, Mandarin"
                value={editingLanguage.language}
                onChange={(e) => setEditingLanguage({ ...editingLanguage, language: e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground">Proficiency</label>
              <Select
                value={editingLanguage.proficiency || ""}
                onValueChange={(value) => setEditingLanguage({ ...editingLanguage, proficiency: value })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select proficiency" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="Native">Native / Bilingual</SelectItem>
                  <SelectItem value="Fluent">Fluent</SelectItem>
                  <SelectItem value="Advanced">Advanced</SelectItem>
                  <SelectItem value="Intermediate">Intermediate</SelectItem>
                  <SelectItem value="Conversational">Conversational</SelectItem>
                  <SelectItem value="Elementary">Elementary</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button size="sm" variant="ghost" onClick={() => setEditingLanguage(null)} disabled={saving}>
              <X className="h-4 w-4 mr-1" /> Cancel
            </Button>
            <Button size="sm" onClick={() => updateLanguage(editingLanguage)} disabled={saving}>
              {saving ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : <Check className="h-4 w-4 mr-1" />} Save
            </Button>
          </div>
        </div>
      );
    }

    return (
      <div className="p-4 rounded-lg border bg-muted/20 flex justify-between items-center">
        <div className="flex items-center gap-3">
          <span className="font-medium">{lang.language || "Untitled"}</span>
          {lang.proficiency && (
            <Badge variant="secondary" className="text-xs">{lang.proficiency}</Badge>
          )}
        </div>
        <div className="flex gap-1">
          <Button size="icon" variant="ghost" className="h-8 w-8" onClick={() => setEditingLanguage(lang)}>
            <Pencil className="h-3 w-3" />
          </Button>
          <Button size="icon" variant="ghost" className="h-8 w-8" onClick={() => deleteLanguage(lang.id)}>
            <Trash2 className="h-3 w-3" />
          </Button>
        </div>
      </div>
    );
  };

  // ==================== Certification Item Component ====================
  const CertificationItem = ({ cert }: { cert: Certification }) => {
    if (editingCertification?.id === cert.id) {
      return (
        <div className="p-4 rounded-lg border bg-background space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-muted-foreground">Certification Name</label>
              <Input
                placeholder="e.g., AWS Solutions Architect"
                value={editingCertification.name}
                onChange={(e) => setEditingCertification({ ...editingCertification, name: e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground">Issuing Organization</label>
              <Input
                placeholder="e.g., Amazon Web Services"
                value={editingCertification.issuer || ""}
                onChange={(e) => setEditingCertification({ ...editingCertification, issuer: e.target.value })}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-muted-foreground">Issue Date</label>
              <Input
                type="date"
                value={editingCertification.issueDate || ""}
                onChange={(e) => setEditingCertification({ ...editingCertification, issueDate: e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground">Expiration Date (optional)</label>
              <Input
                type="date"
                value={editingCertification.expirationDate || ""}
                onChange={(e) => setEditingCertification({ ...editingCertification, expirationDate: e.target.value })}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-muted-foreground">Credential ID (optional)</label>
              <Input
                placeholder="Credential ID"
                value={editingCertification.credentialId || ""}
                onChange={(e) => setEditingCertification({ ...editingCertification, credentialId: e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground">Credential URL (optional)</label>
              <Input
                placeholder="https://..."
                value={editingCertification.credentialUrl || ""}
                onChange={(e) => setEditingCertification({ ...editingCertification, credentialUrl: e.target.value })}
              />
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button size="sm" variant="ghost" onClick={() => setEditingCertification(null)} disabled={saving}>
              <X className="h-4 w-4 mr-1" /> Cancel
            </Button>
            <Button size="sm" onClick={() => updateCertification(editingCertification)} disabled={saving}>
              {saving ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : <Check className="h-4 w-4 mr-1" />} Save
            </Button>
          </div>
        </div>
      );
    }

    return (
      <div className="p-4 rounded-lg border bg-muted/20 flex justify-between items-start">
        <div>
          <h4 className="font-semibold">{cert.name || "Untitled"}</h4>
          {cert.issuer && (
            <p className="text-sm text-muted-foreground">{cert.issuer}</p>
          )}
          {(cert.issueDate || cert.expirationDate) && (
            <p className="text-xs text-muted-foreground mt-1">
              Issued: {cert.issueDate || "N/A"}
              {cert.expirationDate && ` • Expires: ${cert.expirationDate}`}
            </p>
          )}
          {cert.credentialId && (
            <p className="text-xs text-muted-foreground mt-1">ID: {cert.credentialId}</p>
          )}
          {cert.credentialUrl && (
            <a href={cert.credentialUrl} target="_blank" rel="noopener noreferrer" className="text-xs text-primary hover:underline mt-1 block">
              View Credential
            </a>
          )}
        </div>
        <div className="flex gap-1">
          <Button size="icon" variant="ghost" className="h-8 w-8" onClick={() => setEditingCertification(cert)}>
            <Pencil className="h-3 w-3" />
          </Button>
          <Button size="icon" variant="ghost" className="h-8 w-8" onClick={() => deleteCertification(cert.id)}>
            <Trash2 className="h-3 w-3" />
          </Button>
        </div>
      </div>
    );
  };

  return (
    <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
      {/* Work Experience Module */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div className="flex items-center gap-2">
            <Briefcase className="h-5 w-5 text-primary" />
            <CardTitle className="text-lg">Work Experience</CardTitle>
          </div>
          <Button size="sm" variant="outline" onClick={addExperience} disabled={saving}>
            {saving ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : <Plus className="h-4 w-4 mr-1" />}
            Add Experience
          </Button>
        </CardHeader>
        <CardContent className="grid gap-4">
          {experiences.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-4">No work experience yet. Click the button above to add.</p>
          ) : (
            experiences.map(exp => <ExperienceItem key={exp.id} exp={exp} />)
          )}
        </CardContent>
      </Card>

      {/* Education Module */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div className="flex items-center gap-2">
            <GraduationCap className="h-5 w-5 text-primary" />
            <CardTitle className="text-lg">Education</CardTitle>
          </div>
          <Button size="sm" variant="outline" onClick={addEducation} disabled={saving}>
            {saving ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : <Plus className="h-4 w-4 mr-1" />}
            Add Education
          </Button>
        </CardHeader>
        <CardContent className="grid gap-4">
          {educations.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-4">No education yet. Click the button above to add.</p>
          ) : (
            educations.map(edu => <EducationItem key={edu.id} edu={edu} />)
          )}
        </CardContent>
      </Card>

      {/* Skills Module */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div className="flex items-center gap-2">
            <Code className="h-5 w-5 text-primary" />
            <CardTitle className="text-lg">Skills</CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-2 mb-4">
            {skills.length === 0 ? (
              <p className="text-sm text-muted-foreground">No skills yet. Add below.</p>
            ) : (
              skills.map(skill => (
                <Badge key={skill.id} variant="secondary" className="px-3 py-1">
                  {skill.name}
                  <button 
                    className="ml-2 hover:text-destructive"
                    onClick={() => deleteSkill(skill.id)}
                    disabled={saving}
                  >
                    <X className="h-3 w-3" />
                  </button>
                </Badge>
              ))
            )}
          </div>
          <div className="flex gap-2">
            <Input
              placeholder="Add a new skill..."
              value={newSkill}
              onChange={(e) => setNewSkill(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && addSkill()}
            />
            <Button onClick={addSkill} disabled={!newSkill.trim() || saving}>
              {saving ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : <Plus className="h-4 w-4 mr-1" />} Add
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Projects Module */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div className="flex items-center gap-2">
            <FolderGit2 className="h-5 w-5 text-primary" />
            <CardTitle className="text-lg">Projects</CardTitle>
          </div>
          <Button size="sm" variant="outline" onClick={addProject} disabled={saving}>
            {saving ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : <Plus className="h-4 w-4 mr-1" />}
            Add Project
          </Button>
        </CardHeader>
        <CardContent className="grid gap-4">
          {projects.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-4">No projects yet. Click the button above to add.</p>
          ) : (
            projects.map(proj => <ProjectItem key={proj.id} proj={proj} />)
          )}
        </CardContent>
      </Card>

      {/* Certifications Module */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div className="flex items-center gap-2">
            <Award className="h-5 w-5 text-primary" />
            <CardTitle className="text-lg">Certifications & Awards</CardTitle>
          </div>
          <Button size="sm" variant="outline" onClick={addCertification} disabled={saving}>
            {saving ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : <Plus className="h-4 w-4 mr-1" />}
            Add Certification
          </Button>
        </CardHeader>
        <CardContent className="grid gap-4">
          {certifications.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-4">No certifications yet. Click the button above to add.</p>
          ) : (
            certifications.map(cert => <CertificationItem key={cert.id} cert={cert} />)
          )}
        </CardContent>
      </Card>

      {/* Languages Module */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div className="flex items-center gap-2">
            <Languages className="h-5 w-5 text-primary" />
            <CardTitle className="text-lg">Languages</CardTitle>
          </div>
          <Button size="sm" variant="outline" onClick={addLanguage} disabled={saving}>
            {saving ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : <Plus className="h-4 w-4 mr-1" />}
            Add Language
          </Button>
        </CardHeader>
        <CardContent className="grid gap-4">
          {languages.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-4">No languages yet. Click the button above to add.</p>
          ) : (
            languages.map(lang => <LanguageItem key={lang.id} lang={lang} />)
          )}
        </CardContent>
      </Card>
    </div>
  );
}
