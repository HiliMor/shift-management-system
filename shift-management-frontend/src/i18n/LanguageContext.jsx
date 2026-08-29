import { createContext, useContext, useEffect, useState } from "react";
import { translateDomainValue as translateDomainValueForLanguage, translations } from "./translations.js";

const LANGUAGE_STORAGE_KEY = "shift-management-language";
const LanguageContext = createContext(null);

function getInitialLanguage() {
  const storedLanguage = localStorage.getItem(LANGUAGE_STORAGE_KEY);

  return storedLanguage in translations ? storedLanguage : "en";
}

export function LanguageProvider({ children }) {
  const [language, setLanguage] = useState(getInitialLanguage);

  useEffect(() => {
    document.documentElement.lang = language;
    document.documentElement.dir = language === "he" ? "rtl" : "ltr";
    localStorage.setItem(LANGUAGE_STORAGE_KEY, language);
  }, [language]);

  function t(key) {
    return translations[language][key] ?? translations.en[key] ?? key;
  }

  function translateDomainValue(value) {
    return translateDomainValueForLanguage(value, language);
  }

  return (
    <LanguageContext.Provider value={{ language, setLanguage, t, translateDomainValue }}>
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage() {
  const context = useContext(LanguageContext);

  if (!context) {
    throw new Error("useLanguage must be used inside LanguageProvider");
  }

  return context;
}
