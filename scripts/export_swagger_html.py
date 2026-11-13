#!/usr/bin/env python3
"""OpenAPI 문서를 서버 없이 여는 단일 Swagger HTML 파일로 만든다.

사용법:
    ./gradlew test --tests 'com.hmc.hmcbe.global.docs.OpenApiDocumentTest'   # build/openapi/openapi.json 생성
    python3 scripts/export_swagger_html.py [openapi.json] [출력.html]

Swagger UI(swagger-ui-dist)의 JS·CSS를 HTML 안에 넣으므로 인터넷 없이도 열린다.
서버가 없으므로 "Try it out"은 끈다.
"""

import json
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SWAGGER_UI_VERSION = "5.17.14"
SWAGGER_UI_BASE = f"https://cdn.jsdelivr.net/npm/swagger-ui-dist@{SWAGGER_UI_VERSION}/"
CACHE_DIR = ROOT / "build" / "swagger-ui-dist" / SWAGGER_UI_VERSION
DEFAULT_SPEC = ROOT / "build" / "openapi" / "openapi.json"
DEFAULT_OUTPUT = ROOT / "build" / "openapi" / "swagger.html"

TEMPLATE = """<!doctype html>
<html lang="ko">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>__TITLE__</title>
<style>__CSS__</style>
<style>
  body { margin: 0; background: #fafafa; }
  .swagger-ui .topbar { display: none; }
  .swagger-ui .info .description pre { white-space: pre-wrap; }
</style>
</head>
<body>
<div id="swagger-ui"></div>
<script>__JS__</script>
<script>
  const spec = __SPEC__;
  window.ui = SwaggerUIBundle({
    spec,
    dom_id: "#swagger-ui",
    deepLinking: true,
    docExpansion: "list",
    defaultModelsExpandDepth: 0,
    defaultModelExpandDepth: 3,
    displayOperationId: false,
    filter: true,
    supportedSubmitMethods: [],
    presets: [SwaggerUIBundle.presets.apis],
    layout: "BaseLayout"
  });
</script>
</body>
</html>
"""


def fetch(name: str) -> str:
    cached = CACHE_DIR / name
    if not cached.exists():
        CACHE_DIR.mkdir(parents=True, exist_ok=True)
        with urllib.request.urlopen(SWAGGER_UI_BASE + name, timeout=60) as response:
            cached.write_bytes(response.read())
    return cached.read_text(encoding="utf-8")


def inline_safe(text: str) -> str:
    """<script>/<style> 안에 넣어도 태그가 일찍 닫히지 않게 한다."""
    return text.replace("</script", "<\\/script").replace("</style", "<\\/style")


def main() -> None:
    spec_path = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_SPEC
    output_path = Path(sys.argv[2]) if len(sys.argv) > 2 else DEFAULT_OUTPUT
    if not spec_path.exists():
        sys.exit(f"{spec_path}가 없습니다. 먼저 OpenApiDocumentTest를 실행하세요.")

    spec = json.loads(spec_path.read_text(encoding="utf-8"))
    spec.pop("servers", None)
    title = f"{spec.get('info', {}).get('title', 'API')} {spec.get('info', {}).get('version', '')}".strip()
    spec_json = json.dumps(spec, ensure_ascii=False).replace("</", "<\\/")

    html = (TEMPLATE
            .replace("__TITLE__", title)
            .replace("__CSS__", inline_safe(fetch("swagger-ui.css")))
            .replace("__JS__", inline_safe(fetch("swagger-ui-bundle.js")))
            .replace("__SPEC__", spec_json))
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(html, encoding="utf-8")
    print(f"{output_path} ({output_path.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
