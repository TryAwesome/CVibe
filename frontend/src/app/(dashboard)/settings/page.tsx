"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { User, Lock, Cpu, Globe, Upload, Save, Eye, EyeOff, Loader2, AlertCircle, CheckCircle2, ImageIcon, MessageSquare } from "lucide-react";
import { api, AiConfig, User as UserType } from "@/lib/api";
import { useAuth } from "@/lib/contexts/auth-context";

// Model options for Language Models
const LANGUAGE_MODELS = [
  { value: "gpt-4o", label: "OpenAI GPT-4o", provider: "openai" },
  { value: "gpt-4-turbo", label: "OpenAI GPT-4 Turbo", provider: "openai" },
  { value: "gpt-3.5-turbo", label: "OpenAI GPT-3.5 Turbo", provider: "openai" },
  { value: "claude-3-5-sonnet-20241022", label: "Claude 3.5 Sonnet", provider: "anthropic" },
  { value: "claude-3-opus-20240229", label: "Claude 3 Opus", provider: "anthropic" },
  { value: "gemini-pro", label: "Google Gemini Pro", provider: "google" },
  { value: "custom", label: "Custom Model", provider: "custom" },
];

// Model options for Vision Models
const VISION_MODELS = [
  { value: "gpt-4o", label: "OpenAI GPT-4o (Vision)", provider: "openai" },
  { value: "gpt-4-turbo", label: "OpenAI GPT-4 Turbo (Vision)", provider: "openai" },
  { value: "claude-3-5-sonnet-20241022", label: "Claude 3.5 Sonnet (Vision)", provider: "anthropic" },
  { value: "claude-3-opus-20240229", label: "Claude 3 Opus (Vision)", provider: "anthropic" },
  { value: "gemini-pro-vision", label: "Google Gemini Pro Vision", provider: "google" },
  { value: "custom", label: "Custom Vision Model", provider: "custom" },
];

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
  
  // Language Model config state
  const [showLlmApiKey, setShowLlmApiKey] = useState(false);
  const [llmModel, setLlmModel] = useState("gpt-4o");
  const [llmProvider, setLlmProvider] = useState("openai");
  const [llmApiKey, setLlmApiKey] = useState("");
  const [llmBaseUrl, setLlmBaseUrl] = useState("");
  
  // Vision Model config state
  const [showVisionApiKey, setShowVisionApiKey] = useState(false);
  const [visionModel, setVisionModel] = useState("gpt-4o");
  const [visionProvider, setVisionProvider] = useState("openai");
  const [visionApiKey, setVisionApiKey] = useState("");
  const [visionBaseUrl, setVisionBaseUrl] = useState("");
  const [useSameAsLlm, setUseSameAsLlm] = useState(true);
  
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
        // Language Model
        if (res.data.modelName) setLlmModel(res.data.modelName);
        if (res.data.provider) setLlmProvider(res.data.provider);
        if (res.data.apiKey) setLlmApiKey(res.data.apiKey);
        if (res.data.baseUrl) setLlmBaseUrl(res.data.baseUrl);
        // Vision Model
        if (res.data.visionModelName) {
          setVisionModel(res.data.visionModelName);
          setUseSameAsLlm(false);
        }
        if (res.data.visionProvider) setVisionProvider(res.data.visionProvider);
        if (res.data.visionApiKey) setVisionApiKey(res.data.visionApiKey);
        if (res.data.visionBaseUrl) setVisionBaseUrl(res.data.visionBaseUrl);
      }
    } catch (err) {
      console.error('Error fetching AI config:', err);
    }
  };
  
  // Handle LLM model change
  const handleLlmModelChange = (value: string) => {
    setLlmModel(value);
    const selected = LANGUAGE_MODELS.find(m => m.value === value);
    if (selected) {
      setLlmProvider(selected.provider);
    }
  };
  
  // Handle Vision model change
  const handleVisionModelChange = (value: string) => {
    setVisionModel(value);
    const selected = VISION_MODELS.find(m => m.value === value);
    if (selected) {
      setVisionProvider(selected.provider);
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
      const config: any = {
        // Language Model
        provider: llmProvider,
        modelName: llmModel,
        apiKey: llmApiKey,
        baseUrl: llmBaseUrl || undefined,
      };
      
      // Vision Model (use same as LLM if checked)
      if (useSameAsLlm) {
        config.visionProvider = llmProvider;
        config.visionModelName = llmModel;
        config.visionApiKey = llmApiKey;
        config.visionBaseUrl = llmBaseUrl || undefined;
      } else {
        config.visionProvider = visionProvider;
        config.visionModelName = visionModel;
        config.visionApiKey = visionApiKey;
        config.visionBaseUrl = visionBaseUrl || undefined;
      }
      
      const res = await api.updateAiConfig(config);
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
        // Reset Language Model
        setLlmApiKey("");
        setLlmBaseUrl("");
        setLlmModel("gpt-4o");
        setLlmProvider("openai");
        // Reset Vision Model
        setVisionApiKey("");
        setVisionBaseUrl("");
        setVisionModel("gpt-4o");
        setVisionProvider("openai");
        setUseSameAsLlm(true);
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
          <div className="space-y-6">
            {/* Language Model Configuration */}
            <Card>
              <CardHeader>
                <div className="flex items-center gap-2">
                  <MessageSquare className="h-5 w-5" />
                  <CardTitle>Language Model Configuration</CardTitle>
                </div>
                <CardDescription>
                  Configure the language model for text generation, chat, and analysis tasks.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label>Model</Label>
                  <select 
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                    value={llmModel}
                    onChange={(e) => handleLlmModelChange(e.target.value)}
                  >
                    {LANGUAGE_MODELS.map(model => (
                      <option key={model.value} value={model.value}>{model.label}</option>
                    ))}
                  </select>
                </div>

                {llmModel === "custom" && (
                  <div className="space-y-2">
                    <Label htmlFor="llm-model-name">Custom Model Name</Label>
                    <Input 
                      id="llm-model-name" 
                      placeholder="e.g., llama2, mistral, qwen2.5"
                      value={llmModel === "custom" ? "" : llmModel}
                      onChange={(e) => setLlmModel(e.target.value)}
                    />
                  </div>
                )}

                <div className="space-y-2">
                  <Label htmlFor="llm-base-url">Base URL (Optional)</Label>
                  <Input 
                    id="llm-base-url" 
                    placeholder="https://api.openai.com/v1"
                    value={llmBaseUrl}
                    onChange={(e) => setLlmBaseUrl(e.target.value)}
                  />
                  <p className="text-[10px] text-muted-foreground">
                    For proxies or local models (e.g., http://localhost:11434/v1 for Ollama)
                  </p>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="llm-api-key">API Key</Label>
                  <div className="relative">
                    <Input 
                      id="llm-api-key" 
                      type={showLlmApiKey ? "text" : "password"} 
                      placeholder="sk-..." 
                      value={llmApiKey}
                      onChange={(e) => setLlmApiKey(e.target.value)}
                    />
                    <Button 
                      variant="ghost" 
                      size="icon" 
                      className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                      onClick={() => setShowLlmApiKey(!showLlmApiKey)}
                    >
                      {showLlmApiKey ? <EyeOff className="h-4 w-4 text-muted-foreground" /> : <Eye className="h-4 w-4 text-muted-foreground" />}
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Vision Model Configuration */}
            <Card>
              <CardHeader>
                <div className="flex items-center gap-2">
                  <ImageIcon className="h-5 w-5" />
                  <CardTitle>Vision Model Configuration</CardTitle>
                </div>
                <CardDescription>
                  Configure the vision model for image analysis (e.g., resume parsing from images).
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="use-same-as-llm"
                    checked={useSameAsLlm}
                    onChange={(e) => setUseSameAsLlm(e.target.checked)}
                    className="h-4 w-4 rounded border-gray-300"
                  />
                  <Label htmlFor="use-same-as-llm" className="text-sm font-normal">
                    Use same configuration as Language Model
                  </Label>
                </div>

                {!useSameAsLlm && (
                  <>
                    <div className="space-y-2">
                      <Label>Vision Model</Label>
                      <select 
                        className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                        value={visionModel}
                        onChange={(e) => handleVisionModelChange(e.target.value)}
                      >
                        {VISION_MODELS.map(model => (
                          <option key={model.value} value={model.value}>{model.label}</option>
                        ))}
                      </select>
                    </div>

                    {visionModel === "custom" && (
                      <div className="space-y-2">
                        <Label htmlFor="vision-model-name">Custom Model Name</Label>
                        <Input 
                          id="vision-model-name" 
                          placeholder="e.g., llava, bakllava"
                          value={visionModel === "custom" ? "" : visionModel}
                          onChange={(e) => setVisionModel(e.target.value)}
                        />
                      </div>
                    )}

                    <div className="space-y-2">
                      <Label htmlFor="vision-base-url">Base URL (Optional)</Label>
                      <Input 
                        id="vision-base-url" 
                        placeholder="https://api.openai.com/v1"
                        value={visionBaseUrl}
                        onChange={(e) => setVisionBaseUrl(e.target.value)}
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="vision-api-key">API Key</Label>
                      <div className="relative">
                        <Input 
                          id="vision-api-key" 
                          type={showVisionApiKey ? "text" : "password"} 
                          placeholder="sk-..." 
                          value={visionApiKey}
                          onChange={(e) => setVisionApiKey(e.target.value)}
                        />
                        <Button 
                          variant="ghost" 
                          size="icon" 
                          className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                          onClick={() => setShowVisionApiKey(!showVisionApiKey)}
                        >
                          {showVisionApiKey ? <EyeOff className="h-4 w-4 text-muted-foreground" /> : <Eye className="h-4 w-4 text-muted-foreground" />}
                        </Button>
                      </div>
                    </div>
                  </>
                )}
              </CardContent>
            </Card>

            {/* Config Status and Actions */}
            <Card>
              <CardContent className="pt-6">
                {aiConfig && (
                  <div className="p-3 bg-muted/50 rounded-lg mb-4">
                    <p className="text-xs text-muted-foreground">
                      <span className="font-medium">Language Model:</span> {aiConfig.provider} / {aiConfig.modelName}
                      {aiConfig.configured && <span className="ml-2 text-green-600">✓ Configured</span>}
                    </p>
                    {aiConfig.visionConfigured && (
                      <p className="text-xs text-muted-foreground mt-1">
                        <span className="font-medium">Vision Model:</span> {aiConfig.visionProvider} / {aiConfig.visionModelName}
                        <span className="ml-2 text-green-600">✓ Configured</span>
                      </p>
                    )}
                    {aiConfig.updatedAt && (
                      <p className="text-xs text-muted-foreground mt-1">
                        Last updated: {new Date(aiConfig.updatedAt).toLocaleString()}
                      </p>
                    )}
                  </div>
                )}
                
                <div className="flex gap-2">
                  <Button onClick={handleSaveAiConfig} disabled={isLoading}>
                    {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                    Save Configuration
                  </Button>
                  {aiConfig && aiConfig.configured && (
                    <Button variant="outline" onClick={handleDeleteAiConfig} disabled={isLoading}>
                      Delete Config
                    </Button>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}
