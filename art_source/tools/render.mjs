// Renders every .svg in <inDir> to a same-named .png in <outDir> using resvg (keeps alpha).
// Usage: node render.mjs <inDir> <outDir>
import { readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join, basename } from 'node:path';
import { Resvg } from '@resvg/resvg-js';

const [inDir, outDir] = process.argv.slice(2);
if (!inDir || !outDir) {
  console.error('usage: node render.mjs <inDir> <outDir>');
  process.exit(2);
}
for (const file of readdirSync(inDir).filter((f) => f.endsWith('.svg'))) {
  const svg = readFileSync(join(inDir, file), 'utf8');
  const png = new Resvg(svg, { fitTo: { mode: 'original' }, font: { loadSystemFonts: false } }).render().asPng();
  writeFileSync(join(outDir, basename(file, '.svg') + '.png'), png);
}
