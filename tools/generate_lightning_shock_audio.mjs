import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const duration = 0.14;
const frames = Math.round(rate * duration);
const pcm = Buffer.alloc(frames * 2);
let seed = 4541;
let mid = 0;
for (let i = 0; i < frames; i++) {
    const t = i / rate;
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    const noise = seed / 2147483648 - 1;
    mid += (noise - mid) * 0.32;
    const snap = (noise - mid * 0.55) * Math.exp(-t * 82) * 0.79;
    const spark = Math.sin(2 * Math.PI * (927 * t - 1830 * t * t)) * Math.exp(-t * 57) * 0.15;
    const envelope = Math.min(1, t / 0.0018, (duration - t) / 0.034);
    pcm.writeInt16LE(Math.round(Math.tanh((snap + spark) * envelope) * 30300), i * 2);
}

const output = fileURLToPath(new URL(
    '../src/main/resources/assets/narutomod/sounds/jutsu/lightning_shock_snap.ogg', import.meta.url,
));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode Lightning Shock: ${result.status}`);
}
