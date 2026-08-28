import LanguageSelector from "./LanguageSelector.jsx";
import { useLanguage } from "../i18n/LanguageContext.jsx";

function LoginScreen({
  isLoggingIn,
  loginError,
  onLogin,
  onPasswordChange,
  onUsernameChange,
  password,
  username,
}) {
  const { t } = useLanguage();

  return (
    <main className="auth-layout">
      <section className="auth-panel" aria-labelledby="login-title">
        <div className="auth-header">
          <p className="eyebrow">{t("appName")}</p>
          <LanguageSelector />
        </div>
        <h1 id="login-title">{t("signIn")}</h1>

        <form className="login-form" onSubmit={onLogin}>
          <label>
            {t("username")}
            <input
              autoComplete="username"
              name="username"
              onChange={(event) => onUsernameChange(event.target.value)}
              required
              type="text"
              value={username}
            />
          </label>

          <label>
            {t("password")}
            <input
              autoComplete="current-password"
              name="password"
              onChange={(event) => onPasswordChange(event.target.value)}
              required
              type="password"
              value={password}
            />
          </label>

          {loginError ? <p className="error-message">{loginError}</p> : null}

          <button disabled={isLoggingIn} type="submit">
            {isLoggingIn ? t("signingIn") : t("signIn")}
          </button>
        </form>
      </section>
    </main>
  );
}

export default LoginScreen;
