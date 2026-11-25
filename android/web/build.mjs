import { build } from 'esbuild';
import { mkdirSync, copyFileSync } from 'node:fs';
import { resolve } from 'node:path';
const out = resolve('../build/assets');
mkdirSync(out, { recursive: true });
await build({
  stdin: { contents: "import React from 'react'; import {createRoot} from 'react-dom/client'; import App from '../../844.tsx'; createRoot(document.getElementById('root')).render(React.createElement(App));", resolveDir: process.cwd() },
  nodePaths: [resolve('node_modules')], bundle: true, minify: true, target: 'chrome90',
  define: { 'process.env.NODE_ENV': '"production"' }, outfile: out + '/app.js',
});
copyFileSync('index.html', out + '/index.html');
