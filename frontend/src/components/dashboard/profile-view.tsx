"use client";

import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Plus, Pencil, GraduationCap, Briefcase, Trophy, Loader2, Trash2, X, Check, Code } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import api, { Profile, Experience, Skill } from "@/lib/api";

export function ProfileView() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [experiences, setExperiences] = useState<Experience[]>([]);
  const [skills, setSkills] = useState<Skill[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingExperience, setEditingExperience] = useState<Experience | null>(null);
  const [newSkill, setNewSkill] = useState("");

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [profileRes, experiencesRes, skillsRes] = await Promise.all([
        api.getProfile(),
        api.getExperiences(),
        api.getSkills(),
      ]);
      if (profileRes.success && profileRes.data) setProfile(profileRes.data);
      if (experiencesRes.success && experiencesRes.data) setExperiences(experiencesRes.data);
      if (skillsRes.success && skillsRes.data) setSkills(skillsRes.data);
    } catch (error) {
      console.error("Failed to load profile data:", error);
    } finally {
      setLoading(false);
    }
  };

  const addExperience = async (type: "education" | "work" | "award") => {
    try {
      const res = await api.createExperience({
        type,
        title: type === "education" ? "New Education" : type === "work" ? "New Work Experience" : "New Award",
        organization: "",
        startDate: "",
        endDate: "",
        description: "",
      });
      if (res.success && res.data) {
        setExperiences(prev => [...prev, res.data!]);
        setEditingExperience(res.data);
      }
    } catch (error) {
      console.error("Failed to add experience:", error);
    }
  };

  const updateExperience = async (exp: Experience) => {
    try {
      const res = await api.updateExperience(String(exp.id), exp);
      if (res.success) {
        setExperiences(prev => prev.map(e => e.id === exp.id ? exp : e));
        setEditingExperience(null);
      }
    } catch (error) {
      console.error("Failed to update experience:", error);
    }
  };

  const deleteExperience = async (id: number) => {
    try {
      const res = await api.deleteExperience(String(id));
      if (res.success) {
        setExperiences(prev => prev.filter(e => e.id !== id));
      }
    } catch (error) {
      console.error("Failed to delete experience:", error);
    }
  };

  const addSkill = async () => {
    if (!newSkill.trim()) return;
    try {
      const res = await api.createSkill({ name: newSkill, level: "intermediate" });
      if (res.success && res.data) {
        setSkills(prev => [...prev, res.data!]);
        setNewSkill("");
      }
    } catch (error) {
      console.error("Failed to add skill:", error);
    }
  };

  const deleteSkill = async (id: number) => {
    try {
      const res = await api.deleteSkill(String(id));
      if (res.success) {
        setSkills(prev => prev.filter(s => s.id !== id));
      }
    } catch (error) {
      console.error("Failed to delete skill:", error);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  const educations = experiences.filter(e => e.type === "education");
  const works = experiences.filter(e => e.type === "work");
  const awards = experiences.filter(e => e.type === "award");

  const ExperienceItem = ({ exp }: { exp: Experience }) => {
    if (editingExperience?.id === exp.id) {
      return (
        <div className="p-4 rounded-lg border bg-background space-y-3">
          <Input
            placeholder="Title / Position"
            value={editingExperience.title}
            onChange={(e) => setEditingExperience({ ...editingExperience, title: e.target.value })}
          />
          <Input
            placeholder="Organization / Company"
            value={editingExperience.organization || ""}
            onChange={(e) => setEditingExperience({ ...editingExperience, organization: e.target.value })}
          />
          <div className="flex gap-2">
            <Input
              placeholder="Start Date"
              value={editingExperience.startDate || ""}
              onChange={(e) => setEditingExperience({ ...editingExperience, startDate: e.target.value })}
            />
            <Input
              placeholder="End Date"
              value={editingExperience.endDate || ""}
              onChange={(e) => setEditingExperience({ ...editingExperience, endDate: e.target.value })}
            />
          </div>
          <textarea
            placeholder="Description"
            className="w-full p-2 rounded border text-sm min-h-[80px]"
            value={editingExperience.description || ""}
            onChange={(e) => setEditingExperience({ ...editingExperience, description: e.target.value })}
          />
          <div className="flex justify-end gap-2">
            <Button size="sm" variant="ghost" onClick={() => setEditingExperience(null)}>
              <X className="h-4 w-4 mr-1" /> Cancel
            </Button>
            <Button size="sm" onClick={() => updateExperience(editingExperience)}>
              <Check className="h-4 w-4 mr-1" /> Save
            </Button>
          </div>
        </div>
      );
    }

    return (
      <div className="p-4 rounded-lg border bg-muted/20 flex justify-between items-start">
        <div>
          <h4 className="font-semibold">{exp.title || "Untitled"}</h4>
          {exp.organization && (
            <p className="text-sm text-muted-foreground">{exp.organization}</p>
          )}
          {(exp.startDate || exp.endDate) && (
            <p className="text-xs text-muted-foreground mt-1">
              {exp.startDate} {exp.endDate && `- ${exp.endDate}`}
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

  return (
    <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
      {/* Education Module */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div className="flex items-center gap-2">
            <GraduationCap className="h-5 w-5 text-primary" />
            <CardTitle className="text-lg">Education</CardTitle>
          </div>
          <Button size="icon" variant="ghost" onClick={() => addExperience("education")}>
            <Plus className="h-4 w-4" />
          </Button>
        </CardHeader>
        <CardContent className="grid gap-4">
          {educations.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-2">No education history</p>
          ) : (
            educations.map(exp => <ExperienceItem key={exp.id} exp={exp} />)
          )}
        </CardContent>
      </Card>

      {/* Work Experience Module */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div className="flex items-center gap-2">
            <Briefcase className="h-5 w-5 text-primary" />
            <CardTitle className="text-lg">Work Experience</CardTitle>
          </div>
          <Button size="icon" variant="ghost" onClick={() => addExperience("work")}>
            <Plus className="h-4 w-4" />
          </Button>
        </CardHeader>
        <CardContent className="grid gap-4">
          {works.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-2">No work experience</p>
          ) : (
            works.map(exp => <ExperienceItem key={exp.id} exp={exp} />)
          )}
        </CardContent>
      </Card>

      {/* Awards Module */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div className="flex items-center gap-2">
            <Trophy className="h-5 w-5 text-primary" />
            <CardTitle className="text-lg">Awards & Honors</CardTitle>
          </div>
          <Button size="icon" variant="ghost" onClick={() => addExperience("award")}>
            <Plus className="h-4 w-4" />
          </Button>
        </CardHeader>
        <CardContent className="grid gap-4">
          {awards.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-2">No awards</p>
          ) : (
            awards.map(exp => <ExperienceItem key={exp.id} exp={exp} />)
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
            {skills.map(skill => (
              <Badge key={skill.id} variant="secondary" className="px-3 py-1">
                {skill.name}
                <button 
                  className="ml-2 hover:text-destructive"
                  onClick={() => deleteSkill(skill.id)}
                >
                  <X className="h-3 w-3" />
                </button>
              </Badge>
            ))}
          </div>
          <div className="flex gap-2">
            <Input
              placeholder="Add a new skill..."
              value={newSkill}
              onChange={(e) => setNewSkill(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && addSkill()}
            />
            <Button onClick={addSkill} disabled={!newSkill.trim()}>
              <Plus className="h-4 w-4 mr-1" /> Add
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
