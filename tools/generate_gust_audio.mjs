import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const duration = 0.56;
const length = Math.round(rate * duration);
const pcm = Buffer.alloc(length * 2);
let seed = 149890;
let body = 0;
let air = 0;
for (let i = 0; i < length; i++) {
    const t = i / rate;
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    const noise = seed / 2147483648 - 1;
    body += (noise - body) * 0.048;
    air += (noise - air) * (0.27 - t * 0.25);
    const whistle = Math.sin(2 * Math.PI * (405 * t - 229 * t * t)) * 0.052 * Math.exp(-5.3 * t);
    const shape = Math.min(1, t / 0.029, (duration - t) / 0.08) * Math.exp(-t * 3.8);
    const value = Math.tanh(((air - body * 0.72) * 1.46 + body * 1.13 + whistle) * shape);
    pcm.writeInt16LE(Math.round(value * 29000), i * 2);
}

const output = fileURLToPath(new URL(
    '../src/main/resources/assets/narutomod/sounds/jutsu/great_breakthrough_gust.ogg', import.meta.url,
));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode Great Breakthrough: ${result.status}`);
}
