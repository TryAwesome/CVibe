"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { User, Lock, Cpu, Globe, Upload, Save, Eye, EyeOff, Loader2, AlertCircle, CheckCircle2 } from "lucide-react";
import { api, AiConfig, User as UserType } from "@/lib/api";
import { useAuth } from "@/lib/contexts/auth-context";

export default function SettingsPage() {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth();
  
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  
  // Profile state
  const [nickname, setNickname] = useState("");
  
  // Password state
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  
  // AI config state
  const [showApiKey, setShowApiKey] = useState(false);
  const [aiProvider, setAiProvider] = useState("gpt-4");
  const [apiKey, setApiKey] = useState("");
  const [baseUrl, setBaseUrl] = useState("");
  const [aiConfig, setAiConfig] = useState<AiConfig | null>(null);

  // Load user data
  useEffect(() => {
    if (user) {
      setNickname(user.nickname || "");
    }
  }, [user]);

  // Load AI config
  useEffect(() => {
    if (isAuthenticated) {
      fetchAiConfig();
    }
  }, [isAuthenticated]);

  const fetchAiConfig = async () => {
    try {
      const res = await api.getAiConfig();
      if (res.success && res.data) {
        setAiConfig(res.data);
        setAiProvider(res.data.provider || "gpt-4");
        setApiKey(res.data.apiKey || "");
      }
    } catch (err) {
      console.error('Error fetching AI config:', err);
    }
  };

  const handleUpdateProfile = async () => {
    setIsLoading(true);
    setError(null);
    setSuccess(null);
    
    try {
      const res = await api.updateProfile({ nickname });
      if (res.success) {
        setSuccess("Profile updated successfully!");
      } else {
        setError(res.error || "Failed to update profile");
      }
    } catch (err) {
      setError("Failed to update profile");
    } finally {
      setIsLoading(false);
    }
  };

  const handleUpdatePassword = async () => {
    if (newPassword !== confirmPassword) {
      setError("Passwords do not match");
      return;
    }
    if (newPassword.length < 8) {
      setError("Password must be at least 8 characters");
      return;
    }
    
    setIsLoading(true);
    setError(null);
    setSuccess(null);
    
    try {
      const res = await api.updatePassword(currentPassword, newPassword);
      if (res.success) {
        setSuccess("Password updated successfully!");
        setCurrentPassword("");
        setNewPassword("");
        setConfirmPassword("");
      } else {
        setError(res.error || "Failed to update password");
      }
    } catch (err) {
      setError("Failed to update password");
    } finally {
      setIsLoading(false);
    }
  };

  const handleSaveAiConfig = async () => {
    setIsLoading(true);
    setError(null);
    setSuccess(null);
    
    try {
      const res = await api.updateAiConfig({
        provider: aiProvider,
        apiKey: apiKey,
        model: aiProvider,
      });
      if (res.success) {
        setSuccess("AI configuration saved successfully!");
        setAiConfig(res.data || null);
      } else {
        setError(res.error || "Failed to save AI configuration");
      }
    } catch (err) {
      setError("Failed to save AI configuration");
    } finally {
      setIsLoading(false);
    }
  };

  const handleDeleteAiConfig = async () => {
    setIsLoading(true);
    try {
      const res = await api.deleteAiConfig();
      if (res.success) {
        setAiConfig(null);
        setApiKey("");
        setSuccess("AI configuration deleted");
      }
    } catch (err) {
      console.error('Error deleting config:', err);
    } finally {
      setIsLoading(false);
    }
  };

  // Loading state
  if (authLoading) {
    return (
      <div className="h-full flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  // Not authenticated
  if (!isAuthenticated) {
    return (
      <div className="h-full flex flex-col items-center justify-center gap-4">
        <AlertCircle className="h-12 w-12 text-muted-foreground" />
        <p className="text-muted-foreground">Please log in to access settings</p>
      </div>
    );
  }
  
  return (
    <div className="h-full p-8 w-full space-y-8 animate-in fade-in duration-500 overflow-y-auto">
      <div className="space-y-2">
        <h2 className="text-3xl font-bold tracking-tight">Settings</h2>
        <p className="text-muted-foreground">
          Manage your account settings and AI preferences.
        </p>
      </div>

      {/* Status Messages */}
      {error && (
        <div className="bg-destructive/10 text-destructive p-4 rounded-lg flex items-center gap-2">
          <AlertCircle className="h-4 w-4" />
          {error}
        </div>
      )}
      {success && (
        <div className="bg-green-500/10 text-green-600 p-4 rounded-lg flex items-center gap-2">
          <CheckCircle2 className="h-4 w-4" />
          {success}
        </div>
      )}

      <Tabs defaultValue="profile" className="space-y-4">
        <TabsList>
          <TabsTrigger value="profile">Profile</TabsTrigger>
          <TabsTrigger value="security">Security</TabsTrigger>
          <TabsTrigger value="ai">AI Configuration</TabsTrigger>
        </TabsList>
        
        {/* PROFILE TAB */}
        <TabsContent value="profile">
          <Card>
            <CardHeader>
              <CardTitle>Profile</CardTitle>
              <CardDescription>
                Update your personal information.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
                <div className="flex items-center gap-6">
                    <Avatar className="h-20 w-20">
                        <AvatarImage src="/avatars/01.png" alt={user?.nickname || "User"} />
                        <AvatarFallback className="text-lg">
                          {(user?.nickname || user?.email || "U")[0].toUpperCase()}
                        </AvatarFallback>
                    </Avatar>
                    <div className="space-y-2">
                        <Button variant="outline" size="sm">
                            <Upload className="mr-2 h-4 w-4" /> Change Avatar
                        </Button>
                        <p className="text-xs text-muted-foreground">
                            JPG, GIF or PNG. Max size of 800K.
                        </p>
                    </div>
                </div>

                <div className="grid gap-4 md:grid-cols-2">
                    <div className="space-y-2">
                        <Label htmlFor="name">Display Name</Label>
                        <Input 
                          id="name" 
                          value={nickname}
                          onChange={(e) => setNickname(e.target.value)}
                        />
                    </div>
                    <div className="space-y-2">
                        <Label htmlFor="email">Email</Label>
                        <Input 
                          id="email" 
                          value={user?.email || ""} 
                          disabled 
                          className="bg-muted" 
                        />
                    </div>
                </div>
            </CardContent>
            <CardFooter>
                <Button onClick={handleUpdateProfile} disabled={isLoading}>
                  {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                  Save Changes
                </Button>
            </CardFooter>
          </Card>
        </TabsContent>
        
        {/* SECURITY TAB */}
        <TabsContent value="security">
          <Card>
            <CardHeader>
              <CardTitle>Password</CardTitle>
              <CardDescription>
                Change your password.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="current">Current Password</Label>
                <Input 
                  id="current" 
                  type="password" 
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="new">New Password</Label>
                <Input 
                  id="new" 
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="confirm">Confirm Password</Label>
                <Input 
                  id="confirm" 
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                />
              </div>
            </CardContent>
            <CardFooter>
              <Button onClick={handleUpdatePassword} disabled={isLoading}>
                {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                Update Password
              </Button>
            </CardFooter>
          </Card>
        </TabsContent>

        {/* AI CONFIGURATION TAB */}
        <TabsContent value="ai">
          <Card>
            <CardHeader>
              <CardTitle>AI Model Configuration</CardTitle>
              <CardDescription>
                Customize the AI provider settings. Use your own API key for higher limits.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
                <div className="space-y-2">
                    <Label>AI Provider / Model</Label>
                    <select 
                      className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                      value={aiProvider}
                      onChange={(e) => setAiProvider(e.target.value)}
                    >
                        <option value="gpt-4">OpenAI GPT-4 Turbo</option>
                        <option value="gpt-3.5">OpenAI GPT-3.5 Turbo</option>
                        <option value="gemini-pro">Google Gemini Pro</option>
                        <option value="claude-3">Anthropic Claude 3 Opus</option>
                        <option value="custom">Custom (Local LLM)</option>
                    </select>
                </div>

                <div className="space-y-2">
                    <Label htmlFor="base-url">Custom Base URL (Optional)</Label>
                    <Input 
                      id="base-url" 
                      placeholder="https://api.openai.com/v1"
                      value={baseUrl}
                      onChange={(e) => setBaseUrl(e.target.value)}
                    />
                    <p className="text-[10px] text-muted-foreground">Useful for proxies or local models (e.g., http://localhost:11434/v1).</p>
                </div>

                <div className="space-y-2">
                    <Label htmlFor="api-key">API Key</Label>
                    <div className="relative">
                        <Input 
                            id="api-key" 
                            type={showApiKey ? "text" : "password"} 
                            placeholder="sk-..." 
                            value={apiKey}
                            onChange={(e) => setApiKey(e.target.value)}
                        />
                        <Button 
                            variant="ghost" 
                            size="icon" 
                            className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                            onClick={() => setShowApiKey(!showApiKey)}
                        >
                            {showApiKey ? <EyeOff className="h-4 w-4 text-muted-foreground" /> : <Eye className="h-4 w-4 text-muted-foreground" />}
                        </Button>
                    </div>
                </div>

                {aiConfig && (
                  <div className="p-3 bg-muted/50 rounded-lg">
                    <p className="text-xs text-muted-foreground">
                      Current config: {aiConfig.provider} • Configured on {new Date(aiConfig.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                )}
            </CardContent>
            <CardFooter className="flex gap-2">
              <Button onClick={handleSaveAiConfig} disabled={isLoading}>
                {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                Save Configuration
              </Button>
              {aiConfig && (
                <Button variant="outline" onClick={handleDeleteAiConfig} disabled={isLoading}>
                  Delete Config
                </Button>
              )}
            </CardFooter>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
