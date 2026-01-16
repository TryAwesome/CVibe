"use client";

import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Plus, Pencil, Briefcase, Loader2, Trash2, X, Check, Code } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { toast } from "sonner";
import api, { Profile, Experience, Skill, AddExperienceRequest } from "@/lib/api";

export function ProfileView() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [experiences, setExperiences] = useState<Experience[]>([]);
  const [skills, setSkills] = useState<Skill[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingExperience, setEditingExperience] = useState<Experience | null>(null);
  const [newSkill, setNewSkill] = useState("");
  const [saving, setSaving] = useState(false);

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
      toast.error("加载数据失败");
    } finally {
      setLoading(false);
    }
  };

  const addExperience = async () => {
    try {
      setSaving(true);
      const today = new Date().toISOString().split('T')[0]; // yyyy-MM-dd
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
        toast.success("经历已添加");
      } else {
        toast.error(res.error?.message || "添加失败");
      }
    } catch (error) {
      console.error("Failed to add experience:", error);
      toast.error("添加经历失败");
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
        toast.success("经历已更新");
      } else {
        toast.error(res.error?.message || "更新失败");
      }
    } catch (error) {
      console.error("Failed to update experience:", error);
      toast.error("更新经历失败");
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
        toast.success("经历已删除");
      } else {
        toast.error(res.error?.message || "删除失败");
      }
    } catch (error) {
      console.error("Failed to delete experience:", error);
      toast.error("删除经历失败");
    } finally {
      setSaving(false);
    }
  };

  const addSkill = async () => {
    if (!newSkill.trim()) return;
    try {
      setSaving(true);
      const res = await api.createSkill({ name: newSkill, level: "intermediate" });
      if (res.success && res.data) {
        setSkills(prev => [...prev, res.data!]);
        setNewSkill("");
        toast.success("技能已添加");
      } else {
        toast.error(res.error?.message || "添加技能失败");
      }
    } catch (error) {
      console.error("Failed to add skill:", error);
      toast.error("添加技能失败");
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
        toast.success("技能已删除");
      } else {
        toast.error(res.error?.message || "删除技能失败");
      }
    } catch (error) {
      console.error("Failed to delete skill:", error);
      toast.error("删除技能失败");
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

  const ExperienceItem = ({ exp }: { exp: Experience }) => {
    if (editingExperience?.id === exp.id) {
      return (
        <div className="p-4 rounded-lg border bg-background space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-muted-foreground">职位名称</label>
              <Input
                placeholder="Title / Position"
                value={editingExperience.title}
                onChange={(e) => setEditingExperience({ ...editingExperience, title: e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground">公司名称</label>
              <Input
                placeholder="Company Name"
                value={editingExperience.company || ""}
                onChange={(e) => setEditingExperience({ ...editingExperience, company: e.target.value })}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-muted-foreground">地点</label>
              <Input
                placeholder="Location"
                value={editingExperience.location || ""}
                onChange={(e) => setEditingExperience({ ...editingExperience, location: e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground">雇佣类型</label>
              <Select
                value={editingExperience.employmentType || "FULL_TIME"}
                onValueChange={(v) => setEditingExperience({ ...editingExperience, employmentType: v as Experience['employmentType'] })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="FULL_TIME">全职</SelectItem>
                  <SelectItem value="PART_TIME">兼职</SelectItem>
                  <SelectItem value="CONTRACT">合同</SelectItem>
                  <SelectItem value="INTERNSHIP">实习</SelectItem>
                  <SelectItem value="FREELANCE">自由职业</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-muted-foreground">开始日期</label>
              <Input
                type="date"
                value={editingExperience.startDate || ""}
                onChange={(e) => setEditingExperience({ ...editingExperience, startDate: e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground">结束日期</label>
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
            目前仍在职
          </label>
          <div>
            <label className="text-xs text-muted-foreground">描述</label>
            <textarea
              placeholder="Description"
              className="w-full p-2 rounded border text-sm min-h-[80px]"
              value={editingExperience.description || ""}
              onChange={(e) => setEditingExperience({ ...editingExperience, description: e.target.value })}
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button size="sm" variant="ghost" onClick={() => setEditingExperience(null)} disabled={saving}>
              <X className="h-4 w-4 mr-1" /> 取消
            </Button>
            <Button size="sm" onClick={() => updateExperience(editingExperience)} disabled={saving}>
              {saving ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : <Check className="h-4 w-4 mr-1" />} 保存
            </Button>
          </div>
        </div>
      );
    }

    const typeLabel = {
      FULL_TIME: "全职",
      PART_TIME: "兼职",
      CONTRACT: "合同",
      INTERNSHIP: "实习",
      FREELANCE: "自由职业",
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
              {exp.startDate} - {exp.isCurrent ? "至今" : exp.endDate || ""}
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
      {/* Work Experience Module */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div className="flex items-center gap-2">
            <Briefcase className="h-5 w-5 text-primary" />
            <CardTitle className="text-lg">工作经历</CardTitle>
          </div>
          <Button size="sm" variant="outline" onClick={addExperience} disabled={saving}>
            {saving ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : <Plus className="h-4 w-4 mr-1" />}
            添加经历
          </Button>
        </CardHeader>
        <CardContent className="grid gap-4">
          {experiences.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-4">暂无工作经历，点击上方按钮添加</p>
          ) : (
            experiences.map(exp => <ExperienceItem key={exp.id} exp={exp} />)
          )}
        </CardContent>
      </Card>

      {/* Skills Module */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div className="flex items-center gap-2">
            <Code className="h-5 w-5 text-primary" />
            <CardTitle className="text-lg">技能</CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-2 mb-4">
            {skills.length === 0 ? (
              <p className="text-sm text-muted-foreground">暂无技能，请在下方添加</p>
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
              placeholder="添加新技能..."
              value={newSkill}
              onChange={(e) => setNewSkill(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && addSkill()}
            />
            <Button onClick={addSkill} disabled={!newSkill.trim() || saving}>
              {saving ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : <Plus className="h-4 w-4 mr-1" />} 添加
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
