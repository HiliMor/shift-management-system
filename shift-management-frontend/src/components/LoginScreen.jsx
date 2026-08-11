function LoginScreen({
  isLoggingIn,
  loginError,
  onLogin,
  onPasswordChange,
  onUsernameChange,
  password,
  username,
}) {
  return (
    <main className="auth-layout">
      <section className="auth-panel" aria-labelledby="login-title">
        <p className="eyebrow">Shift Management</p>
        <h1 id="login-title">Sign in</h1>

        <form className="login-form" onSubmit={onLogin}>
          <label>
            Username
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
            Password
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
            {isLoggingIn ? "Signing in..." : "Sign in"}
          </button>
        </form>
      </section>
    </main>
  );
}

export default LoginScreen;
