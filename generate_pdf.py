import os
import re
import subprocess
import markdown
import pymupdf

def generate_clean_report():
    with open("PROJECT_REPORT.md", "r", encoding="utf-8") as f:
        md_text = f.read()

    # Split metadata from cover page
    cover_match = re.search(r"^## 1\. Cover Page(.*?)(?=^## 2\. Introduction)", md_text, re.MULTILINE | re.DOTALL)
    cover_content = cover_match.group(1).strip() if cover_match else ""

    metadata = {}
    for line in cover_content.splitlines():
        if ":" in line:
            clean_line = line.strip().lstrip("*").strip()
            parts = clean_line.split(":", 1)
            if len(parts) == 2:
                key = parts[0].replace("*", "").strip()
                val = parts[1].replace("*", "").strip()
                metadata[key] = val

    # Body markdown (without the cover section)
    body_md = re.sub(r"^# PROJECT REPORT:.*?\n## 1\. Cover Page.*?(?=## 2\. Introduction)", "", md_text, flags=re.DOTALL)

    # Convert markdown to HTML
    html_body = markdown.markdown(body_md, extensions=['tables', 'fenced_code', 'nl2br'])

    # Convert mermaid code blocks into standalone styled cards
    def wrap_mermaid(match):
        code = match.group(1)
        code = code.replace("&gt;", ">").replace("&lt;", "<").replace("&amp;", "&").replace("&quot;", '"')
        return f'<div class="diagram-box"><pre class="mermaid">{code}</pre></div>'

    html_body = re.sub(
        r'<pre><code class="language-mermaid">(.*?)</code></pre>',
        wrap_mermaid,
        html_body,
        flags=re.DOTALL
    )

    html_doc = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>LogPulse - Project Report</title>
<script type="module">
  import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
  mermaid.initialize({{
    startOnLoad: true,
    theme: 'neutral',
    flowchart: {{ useMaxWidth: true, htmlLabels: true, curve: 'basis' }},
    sequence: {{ useMaxWidth: true }},
    themeVariables: {{
      fontSize: '11px',
      primaryColor: '#eff6ff',
      primaryBorderColor: '#3b82f6',
      lineColor: '#475569'
    }}
  }});
</script>
<style>
  @page {{
    size: A4 portrait;
    margin: 18mm 16mm 18mm 16mm;
  }}

  body {{
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
    font-size: 9.5pt;
    line-height: 1.55;
    color: #1e293b;
    margin: 0;
    padding: 0;
  }}

  /* Cover Page */
  .cover-page {{
    page-break-after: always;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    height: 90vh;
    padding: 30px 10px 10px 10px;
    border-left: 6px solid #1e3a8a;
  }}

  .cover-institution {{
    font-size: 13pt;
    font-weight: 800;
    letter-spacing: 1.5px;
    color: #1e3a8a;
  }}

  .cover-dept {{
    font-size: 10pt;
    color: #64748b;
    margin-top: 4px;
    font-weight: 500;
  }}

  .cover-badge {{
    display: inline-block;
    background-color: #dbeafe;
    color: #1e40af;
    padding: 4px 14px;
    border-radius: 9999px;
    font-size: 8.5pt;
    font-weight: 700;
    margin-bottom: 16px;
    letter-spacing: 0.5px;
  }}

  .cover-title {{
    font-size: 32pt;
    font-weight: 800;
    line-height: 1.15;
    color: #0f172a;
    margin: 0 0 12px 0;
  }}

  .cover-subtitle {{
    font-size: 13pt;
    color: #475569;
    line-height: 1.45;
    margin-bottom: 25px;
    max-width: 620px;
  }}

  .cover-meta-grid {{
    background-color: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 20px;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px 24px;
    margin-top: 20px;
  }}

  .meta-label {{
    font-size: 8pt;
    text-transform: uppercase;
    letter-spacing: 0.6px;
    color: #64748b;
    font-weight: 700;
  }}

  .meta-value {{
    font-size: 10pt;
    color: #0f172a;
    font-weight: 600;
    margin-top: 2px;
  }}

  .cover-footer {{
    font-size: 8.5pt;
    color: #94a3b8;
    border-top: 1px solid #e2e8f0;
    padding-top: 12px;
  }}

  /* Content */
  h2 {{
    font-size: 13pt;
    font-weight: 700;
    color: #1e3a8a;
    border-bottom: 2px solid #e2e8f0;
    padding-bottom: 4px;
    margin-top: 24px;
    margin-bottom: 10px;
    page-break-after: avoid;
  }}

  h3 {{
    font-size: 10.5pt;
    font-weight: 700;
    color: #0f172a;
    margin-top: 16px;
    margin-bottom: 6px;
    page-break-after: avoid;
  }}

  p, ul, ol {{
    margin-top: 0;
    margin-bottom: 8px;
  }}

  li {{
    margin-bottom: 3px;
  }}

  table {{
    width: 100%;
    border-collapse: collapse;
    margin: 12px 0;
    font-size: 8.5pt;
    page-break-inside: avoid;
  }}

  th, td {{
    padding: 6px 8px;
    text-align: left;
    border: 1px solid #cbd5e1;
  }}

  th {{
    background-color: #1e3a8a;
    color: #ffffff;
    font-weight: 600;
  }}

  tr:nth-child(even) {{
    background-color: #f8fafc;
  }}

  pre {{
    background-color: #0f172a;
    color: #f8fafc;
    padding: 10px 12px;
    border-radius: 6px;
    font-family: Consolas, 'Courier New', Courier, monospace;
    font-size: 7.5pt;
    line-height: 1.35;
    overflow-x: auto;
    page-break-inside: avoid;
    margin: 10px 0;
  }}

  code {{
    font-family: Consolas, 'Courier New', Courier, monospace;
    background-color: #f1f5f9;
    color: #0f172a;
    padding: 1px 4px;
    border-radius: 3px;
    font-size: 8.5pt;
  }}

  pre code {{
    background: transparent;
    color: inherit;
    padding: 0;
  }}

  /* Diagram Cards */
  .diagram-box {{
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 12px;
    margin: 14px 0;
    text-align: center;
    page-break-inside: avoid;
    clear: both;
  }}

  .diagram-box pre.mermaid {{
    background: transparent;
    color: #0f172a;
    padding: 0;
    margin: 0;
    display: flex;
    justify-content: center;
  }}

  .diagram-box svg {{
    max-width: 100% !important;
    max-height: 360px !important;
    height: auto !important;
    margin: 0 auto !important;
    display: block !important;
  }}
</style>
</head>
<body>

<div class="cover-page">
  <div>
    <div class="cover-institution">{metadata.get('Institution', 'VIT Bhopal University')}</div>
    <div class="cover-dept">School of Computing Science and Engineering</div>
  </div>

  <div>
    <div class="cover-badge">{metadata.get('Academic Component', 'Evaluated Course Project')}</div>
    <h1 class="cover-title">LogPulse</h1>
    <div class="cover-subtitle">Multi-Threaded Server Log Anomaly & Rate Limiter Engine in Java</div>

    <div class="cover-meta-grid">
      <div>
        <div class="meta-label">Course Title</div>
        <div class="meta-value">{metadata.get('Course Title', 'Programming in Java')}</div>
      </div>
      <div>
        <div class="meta-label">Platform</div>
        <div class="meta-value">{metadata.get('Platform', 'VITyarthi Learning Destination')}</div>
      </div>
      <div>
        <div class="meta-label">Submission Date</div>
        <div class="meta-value">{metadata.get('Submission Date', 'September 2026')}</div>
      </div>
      <div>
        <div class="meta-label">Runtime / Environment</div>
        <div class="meta-value">{metadata.get('Language & Runtime', 'Java SE 17+ (JDK 26 Verified)')}</div>
      </div>
      <div>
        <div class="meta-label">Student Author</div>
        <div class="meta-value">{metadata.get('Author', 'Dhrrishit V Deka')}</div>
      </div>
      <div>
        <div class="meta-label">Contact Email</div>
        <div class="meta-value">{metadata.get('Contact Email', 'n9yyk6uuu@mozmail.com')}</div>
      </div>
    </div>
  </div>

  <div class="cover-footer">
    Evaluated Project Report &mdash; VITyarthi Platform &bull; Academic Year 2026-2027
  </div>
</div>

<div class="report-content">
{html_body}
</div>

</body>
</html>
"""

    with open("target/clean_report.html", "w", encoding="utf-8") as f:
        f.write(html_doc)

    edge_exe = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
    abs_html = os.path.abspath("target/clean_report.html")
    abs_pdf = os.path.abspath("PROJECT_REPORT.pdf")

    cmd = [
        edge_exe,
        "--headless=new",
        "--disable-gpu",
        "--no-pdf-header-footer",
        "--virtual-time-budget=7000",
        "--run-all-compositor-stages-before-draw",
        f"--print-to-pdf={abs_pdf}",
        f"file:///{abs_html.replace(os.sep, '/')}"
    ]

    subprocess.run(cmd, check=True)

    doc = pymupdf.open(abs_pdf)
    doc.save("LogPulse_Project_Report.pdf")
    print(f"Generated clean report with {len(doc)} pages.")
    doc.close()

if __name__ == "__main__":
    generate_clean_report()
