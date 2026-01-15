"use client";

import React, { createContext, useContext, useState, useEffect } from "react";

type Language = "en";

type LanguageContextType = {
  language: Language;
  setLanguage: (lang: Language) => void;
  t: (key: string) => string;
};

// English-only translations
const translations: Record<string, string> = {
  "Dashboard": "Dashboard",
  "Interview": "Interview",
  "Resume Builder": "Resume Builder",
  "Job Recommendations": "Job Recommendations",
  "Community": "Community",
  "Career Growth": "Career Growth",
  "Mock Interview": "Mock Interview",
  "Settings": "Settings",
};

const LanguageContext = createContext<LanguageContextType | undefined>(undefined);

export function LanguageProvider({ children }: { children: React.ReactNode }) {
  const [language, setLanguage] = useState<Language>("en");

  // Load from localStorage on mount
  useEffect(() => {
    const saved = localStorage.getItem("language") as Language;
    if (saved) setLanguage(saved);
  }, []);

  // Save to localStorage on change
  useEffect(() => {
    localStorage.setItem("language", language);
  }, [language]);

  const t = (key: string) => {
    return translations[key] || key;
  };

  return (
    <LanguageContext.Provider value={{ language, setLanguage, t }}>
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage() {
  const context = useContext(LanguageContext);
  if (context === undefined) {
    throw new Error("useLanguage must be used within a LanguageProvider");
  }
  return context;
}
