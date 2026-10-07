# Additional Tools

The `tools/` directory contains external debugging and screenshot tools for development. They don't run with the mod; invoke them manually when needed:

```text
tools/
├── kltytonui-debug-client.mjs   Node.js client for the external debug protocol
├── kltytonui-mcp/               MCP stdio bridge (lets MCP tools operate the running page directly)
├── kui_compare.py              PNG visual diff comparison
└── kui_preview_loop.ps1        loops launching the client and comparing screenshots
```

Environment: the Node client needs Node 22+; MCP needs Node 20+ plus `npm install` first; the comparator needs Python 3 + Pillow (`python -m pip install Pillow`). Run commands from the repository root (the preview script invokes `./gradlew.bat`).

## Enabling the External Debug Service

All the tool chains connect to the in-game debug service at the fixed address `ws://127.0.0.1:25321/kltytonui`. How to enable it (enabled by default in dev environments):

```toml
# config/kltytonui-client.toml
[debug]
remoteDebug = true
```

Once the service starts, it writes `run/kltytonui/debug.json` in the game directory (endpoint + token + pid). The token is randomly generated on each launch; pin it with `-Dkltytonui.debug.token=...`, or disable the service with `-Dkltytonui.debug.enabled=false`.

**debug.json is an access credential** — whoever has it can drive the real UI of the running client. Don't commit it, don't share it. When you can't connect, check first whether this file exists.

## kltytonui-debug-client.mjs

A lightweight Node client that wraps protocol calls:

```js
import { connect } from "./tools/kltytonui-debug-client.mjs";

const client = await connect();          // reads run/kltytonui/debug.json by default
try {
    const targets = await client.documents();
    const page = await client.attach(targets[0].targetId);   // targetId is a full UUID
    try {
        const button = page.locator("#save");
        await button.waitFor({ state: "visible", timeout: 5000 });
        console.log(await button.attributes());
        await button.click();
    } finally {
        await page.detach();
    }
} finally {
    client.close();
}
```

- `connect({ discoveryFile, endpoint, token, timeout })`: when giving endpoint explicitly, you must also give token;
- Client: `info()` / `documents()` / `attach(targetId)` / `call(method, params)` / `close()`;
- Page: `locator(selector)` / `snapshot({maxDepth, maxNodes})` / `detach()`;
- Locator: `count/attributes/text/computedStyle/boxModel/hover/click/fill(value)/waitFor({state})`, where state is one of attached/detached/visible/hidden. Locator re-queries the selector on every operation, so it doesn't need to be rebuilt after a page refresh;
- Don't hard-code targetId — the UUID changes when the page reloads or closes; call `documents()` first each time.

Capability boundaries: only DOM queries, style/box-model inspection, and limited input operations. **No evaluate, no arbitrary JS execution, no file read/write, no screenshots**. To run page scripts, modify the test page or use page logs.

## kltytonui-mcp

An MCP stdio bridge that lets MCP-capable tools directly view and operate the running KUI:

```powershell
cd tools/kltytonui-mcp
npm install
npm test
```

MCP client configuration:

```json
{
  "mcpServers": {
    "kltytonui": {
      "command": "node",
      "args": ["D:/work/KUI/tools/kltytonui-mcp/server.mjs"],
      "env": { "KLTYTONUI_DEBUG_DISCOVERY": "D:/work/KUI/run/kltytonui/debug.json" }
    }
  }
}
```

You can also bypass the discovery file by setting `KLTYTONUI_DEBUG_ENDPOINT` + `KLTYTONUI_DEBUG_TOKEN` directly (the two must be set as a pair).

Provided tools: `kltytonui_documents` (list targets) → `kltytonui_snapshot` / `kltytonui_query` / `kltytonui_inspect` (query the DOM) → `kltytonui_wait_for` / `kltytonui_hover` / `kltytonui_click` / `kltytonui_fill` (interaction). Queries have count/depth limits (snapshot defaults to 32 levels and 5000 nodes, query to 20 entries); failures return `isError: true` without crashing the process.

## kui_compare.py: Screenshot Comparison

```powershell
python tools/kui_compare.py <reference.png> <actual.png>
```

The actual image is center-cropped to the reference's aspect ratio, Lanczos-scaled to the same size, then the RGB root-mean-square error is computed. By default it judges similarity as: `rms < 70` and `dark_ratio < 0.55` and `avg_luma > 40` (the latter two filter out all-black/empty screenshots).

Output is JSON (`ok` + all metrics); exit code 0 = similar, 1 = not similar or file missing, 2 = bad arguments. It is not a strict pixel-by-pixel comparison; for strict equality additionally require `rms == 0` in the output.

## kui_preview_loop.ps1: Launch-Screenshot Loop

Repeatedly launches the client, takes the latest screenshot, and compares it against the reference image — for tuning unstable launch timing or resource loading:

```powershell
pwsh -File tools/kui_preview_loop.ps1 -MaxAttempts 8 -AutoExitSeconds 8
```

- Parameters: `MaxAttempts` (default 5), `AutoExitSeconds` (default 5), `ViewportWidth/Height` (default 427×249);
- Dependencies: reference image `run/kltytonui/kltytonui/example.png`, screenshot directory `run/screenshots/kui/`;
- Each attempt runs `./gradlew.bat runClient` (with auto-exit and viewport parameters), then compares the newest PNG in the screenshot directory; it exits 0 immediately on a match and throws if all attempts fail. The script verifies that this launch actually produced a new screenshot — it won't pass off a stale one.

If the reference image is missing, first confirm example.png exists; if it reports the screenshot wasn't updated, check whether the client auto-exited and wrote a screenshot as expected.

## Notes

- The debug interface can drive real UI input — don't expose the token to untrusted scripts;
- `run/kltytonui/debug.json` and screenshots are local artifacts — don't commit them;
- `tools/kltytonui-mcp/node_modules` is an npm artifact — don't hand-edit it; to update dependencies, change package.json and rerun `npm install`.
