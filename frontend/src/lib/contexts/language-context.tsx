"use client";

import React, { createContext, useContext, useState, useEffect } from "react";

type Language = "en" | "zh";

type LanguageContextType = {
  language: Language;
  setLanguage: (lang: Language) => void;
  t: (key: string) => string;
};

const translations: Record<string, Record<Language, string>> = {
  "Dashboard": { en: "Dashboard", zh: "仪表盘" },
  "Interview": { en: "Interview", zh: "AI 访谈" },
  "Resume Builder": { en: "Resume Builder", zh: "简历制作" },
  "Job Recommendations": { en: "Job Recommendations", zh: "职位推荐" },
  "Community": { en: "Community", zh: "社区" },
  "Career Growth": { en: "Career Growth", zh: "职业成长" },
  "Mock Interview": { en: "Mock Interview", zh: "模拟面试" },
  "Settings": { en: "Settings", zh: "设置" },
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
    return translations[key]?.[language] || key;
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
