import { useLanguage } from "../i18n/LanguageContext.jsx";

function LanguageSelector() {
  const { language, setLanguage, t } = useLanguage();

  return (
    <label className="language-selector">
      {t("language")}
      <select
        aria-label={t("language")}
        onChange={(event) => setLanguage(event.target.value)}
        value={language}
      >
        <option value="en">{t("english")}</option>
        <option value="he">{t("hebrew")}</option>
      </select>
    </label>
  );
}

export default LanguageSelector;

