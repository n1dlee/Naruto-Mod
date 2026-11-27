import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const duration = 0.61;
const frames = Math.round(duration * rate);
const pcm = Buffer.alloc(frames * 2);
let seed = 13081;
let body = 0;
let low = 0;
for (let i = 0; i < frames; i++) {
    const t = i / rate;
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    const noise = seed / 2147483648 - 1;
    body += (noise - body) * (0.19 - 0.12 * t);
    low += (noise - low) * 0.018;
    const sweep = (body - low) * Math.sin(Math.PI * t / duration) ** 1.5;
    const thump = Math.sin(2 * Math.PI * (103 * t - 42 * t * t)) * Math.exp(-7 * t) * 0.29;
    const shaft = Math.sin(2 * Math.PI * 435 * t) * Math.exp(-26 * t) * 0.12;
    const soft = Math.min(1, t / 0.031, (duration - t) / 0.077);
    pcm.writeInt16LE(Math.round(Math.tanh((sweep * 1.79 + low * 0.77 + thump + shaft) * soft) * 29600), i * 2);
}

const output = fileURLToPath(new URL('../src/main/resources/assets/narutomod/sounds/jutsu/gunbai_wind_cut.ogg', import.meta.url));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode Gunbai: ${result.status}`);
}
