
import { useEffect, useState } from "react";
import "./App.css";

const API = "https://every-clocks-coleman-upgrading.trycloudflare.com/api";
const presets = ["https://example.com", "https://www.google.com"];

function App() {
  const [url, setUrl] = useState("https://example.com");
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [active, setActive] = useState("Overview");
  const [lastChecked, setLastChecked] = useState(null);
  const [history, setHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [historyError, setHistoryError] = useState("");

  async function loadHistory() {
    setHistoryLoading(true);
    setHistoryError("");

    try {
      const response = await fetch(`${API}/history`);
      if (!response.ok) throw new Error("History request failed");

      const data = await response.json();
      setHistory(Array.isArray(data) ? data : []);
    } catch {
      setHistoryError("Unable to load history. Check the backend connection.");
    } finally {
      setHistoryLoading(false);
    }
  }

  useEffect(() => {
    loadHistory();
  }, []);

  async function checkWebsite(event) {
    event?.preventDefault();
    setLoading(true);
    setError("");

    try {
      const response = await fetch(
        `${API}/check?url=${encodeURIComponent(url)}`
      );

      if (!response.ok) throw new Error("Website check request failed");

      const data = await response.json();
      setResult(data);
      setLastChecked(new Date());

      // Reload saved records after every successful API response.
      await loadHistory();
    } catch {
      setError("Unable to connect. Check that the backend is running.");
    } finally {
      setLoading(false);
    }
  }

  const isUp = result?.status === "UP";

  function formatDate(value) {
    if (!value) return "—";

    const date = new Date(value);
    return Number.isNaN(date.getTime())
      ? value
      : date.toLocaleString();
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#overview">
          <span className="brand-mark"><span>U</span></span>
          <span>
            <strong>uptimewatch</strong>
            <small>MONITORING CLOUD</small>
          </span>
        </a>

        <p className="nav-label">WORKSPACE</p>

        {["Overview", "Monitors", "Incidents"].map((item, index) => (
          <button
            key={item}
            className={`nav-item ${active === item ? "selected" : ""}`}
            onClick={() => setActive(item)}
          >
            <span className="nav-symbol">{["◫", "◎", "◷"][index]}</span>
            {item}
            {item === "Monitors" && history.length > 0 && (
              <span className="nav-count">{history.length}</span>
            )}
          </button>
        ))}

        <div className="sidebar-bottom">
          <div className="cloud-card">
            <span className="cloud-icon">☁</span>
            <strong>Cloud-ready</strong>
            <p>Built for reliable website monitoring.</p>
            <span className="local-tag">LOCAL DEVELOPMENT</span>
          </div>

          <button
            className={`nav-item ${active === "Settings" ? "selected" : ""}`}
            onClick={() => setActive("Settings")}
          >
            <span className="nav-symbol">⚙</span>
            Settings
          </button>

          <div className="profile">
            <div className="avatar">UW</div>
            <div>
              <strong>Workspace</strong>
              <small>Personal project</small>
            </div>
          </div>
        </div>
      </aside>

      <main className="main-panel" id="overview">
        <header className="topbar">
          <div className="breadcrumb">Workspace <span>/</span> {active}</div>
          <div className="top-right">
            <span className="environment"><i /> Local environment</span>
            <div className="top-avatar">U</div>
          </div>
        </header>

        <div className="page-content">
          <section className="page-heading">
            <div>
              <p className="eyebrow">YOUR MONITORING SPACE</p>
              <h1>{active === "Overview" ? "Good evening." : active}</h1>
              <p className="muted">
                {active === "Overview"
                  ? "A clearer view of your website health."
                  : active === "Monitors"
                  ? "Check a website and inspect its saved history."
                  : active === "Incidents"
                  ? "Review recent check results and failures."
                  : "UptimeWatch workspace preferences."}
              </p>
            </div>

            <button
              className="primary-button heading-button"
              onClick={() => document.getElementById("monitor-url")?.focus()}
            >
              + New check
            </button>
          </section>

          <section className="metrics-grid">
            <article className="metric-card">
              <div className="metric-top">
                <span>Latest status</span>
                <span className="metric-icon green">✓</span>
              </div>
              <h2 className={result ? (isUp ? "green-text" : "red-text") : ""}>
                {result ? result.status : "—"}
              </h2>
              <p>
                {result ? `HTTP ${result.httpStatus ?? "—"}` : "Run your first check"}
              </p>
            </article>

            <article className="metric-card">
              <div className="metric-top">
                <span>Response time</span>
                <span className="metric-icon blue">↗</span>
              </div>
              <h2>
                {result ? result.responseTimeMs : "—"}
                <small>{result ? " ms" : ""}</small>
              </h2>
              <p>Latest request duration</p>
            </article>

            <article className="metric-card">
              <div className="metric-top">
                <span>Saved checks</span>
                <span className="metric-icon violet">◎</span>
              </div>
              <h2>{history.length}</h2>
              <p>Records in database</p>
            </article>
          </section>

          <section className="monitor-layout">
            <article className="panel check-panel">
              <div className="panel-heading">
                <div>
                  <h3>Run a health check</h3>
                  <p>Enter a website URL to inspect its availability.</p>
                </div>
                <span className="panel-icon">⌁</span>
              </div>

              <form onSubmit={checkWebsite}>
                <label htmlFor="monitor-url">Website address</label>
                <div className="input-wrap">
                  <span>↗</span>
                  <input
                    id="monitor-url"
                    type="url"
                    required
                    value={url}
                    onChange={(event) => setUrl(event.target.value)}
                    placeholder="https://yourwebsite.com"
                  />
                </div>
                <button className="primary-button run-button" disabled={loading}>
                  {loading ? "Checking website..." : "Run health check  →"}
                </button>
              </form>

              <div className="quick-links">
                <span>Try a sample:</span>
                {presets.map((preset) => (
                  <button
                    type="button"
                    key={preset}
                    onClick={() => setUrl(preset)}
                  >
                    {preset.replace("https://", "")}
                  </button>
                ))}
              </div>

              {error && <p className="error-message">{error}</p>}
            </article>

            <article className="panel result-panel">
              <div className="panel-heading">
                <div>
                  <h3>Latest result</h3>
                  <p>Most recent manual health check</p>
                </div>
                <span
                  className={`status-pill ${
                    result ? (isUp ? "pill-up" : "pill-down") : "pill-idle"
                  }`}
                >
                  <i /> {result ? result.status : "NOT CHECKED"}
                </span>
              </div>

              {result ? (
                <>
                  <div className="result-url">{result.url}</div>
                  <div className="result-detail">
                    <span>HTTP response code</span>
                    <strong>{result.httpStatus ?? "No response"}</strong>
                  </div>
                  <div className="result-detail">
                    <span>Response duration</span>
                    <strong>{result.responseTimeMs} ms</strong>
                  </div>
                  <div className="result-detail">
                    <span>Checked at</span>
                    <strong>{lastChecked?.toLocaleTimeString() ?? "—"}</strong>
                  </div>
                </>
              ) : (
                <div className="empty-state">
                  <div className="empty-icon">◎</div>
                  <strong>No checks in this session</strong>
                  <p>Run a check to display its result here.</p>
                </div>
              )}
            </article>
          </section>

          <section className="panel activity-panel">
            <div className="panel-heading">
              <div>
                <h3>Recent monitoring history</h3>
                <p>Latest saved checks from your MySQL database</p>
              </div>
              <button
                type="button"
                className="activity-label"
                onClick={loadHistory}
                disabled={historyLoading}
              >
                {historyLoading ? "LOADING..." : "↻ REFRESH"}
              </button>
            </div>

            {historyError ? (
              <div className="activity-empty">
                {historyError}
                <button type="button" onClick={loadHistory}>Try again</button>
              </div>
            ) : historyLoading && history.length === 0 ? (
              <div className="activity-empty">Loading saved history...</div>
            ) : history.length === 0 ? (
              <div className="activity-empty">
                No saved checks yet. Run your first website check.
              </div>
            ) : (
              <div className="history-table-wrap">
                <table className="history-table">
                  <thead>
                    <tr>
                      <th>Website</th>
                      <th>Status</th>
                      <th>HTTP</th>
                      <th>Response</th>
                      <th>Checked at</th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.map((item) => (
                      <tr key={item.id}>
                        <td className="history-url">{item.url}</td>
                        <td>
                          <span
                            className={`status-pill ${
                              item.status === "UP" ? "pill-up" : "pill-down"
                            }`}
                          >
                            <i /> {item.status}
                          </span>
                        </td>
                        <td>{item.httpStatus ?? "—"}</td>
                        <td>{item.responseTimeMs} ms</td>
                        <td>{formatDate(item.checkedAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <footer className="footer">
            <span>© 2026 UptimeWatch</span>
            <span>React <b>·</b> Spring Boot <b>·</b> AWS-ready</span>
          </footer>
        </div>
      </main>
    </div>
  );
}

export default App;
