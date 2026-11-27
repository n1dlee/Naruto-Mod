import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const duration = 0.94;
const pcm = Buffer.alloc(Math.round(rate * duration) * 2);
let seed = 8919;
let low = 0;
let edge = 0;
for (let i = 0; i < pcm.length / 2; i++) {
    const t = i / rate;
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    const white = seed / 2147483648 - 1;
    low += (white - low) * 0.025;
    edge += (white - edge) * (0.22 - t * 0.15);
    const spiral = ((edge - low) * 0.75 + low * 1.08) * (0.55 + 0.45 * Math.cos(21 * t + 18 * t ** 2));
    const body = Math.sin(2 * Math.PI * (115 * t - 38 * t ** 2)) * 0.30 * Math.exp(-((t - 0.49) ** 2) * 18);
    const tail = Math.sin(2 * Math.PI * 71 * t) * 0.14 * Math.exp(-t * 2.3);
    const env = Math.min(1, t / 0.12, (duration - t) / 0.19);
    pcm.writeInt16LE(Math.round(Math.tanh((spiral * 1.4 + body + tail) * env) * 29400), i * 2);
}

const output = fileURLToPath(new URL('../src/main/resources/assets/narutomod/sounds/jutsu/kamui_transit.ogg', import.meta.url));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode Kamui transit: ${result.status}`);
}
