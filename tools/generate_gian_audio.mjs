import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const duration = 0.38;
const frames = Math.round(rate * duration);
const pcm = Buffer.alloc(frames * 2);
let seed = 327597;
let low = 0;
let mid = 0;
for (let i = 0; i < frames; i++) {
    const t = i / rate;
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    const noise = seed / 2147483648 - 1;
    low += (noise - low) * 0.065;
    mid += (noise - mid) * 0.38;
    const envelope = Math.min(1, t / 0.003, (duration - t) / 0.045);
    const snap = (noise - mid * 0.68) * Math.exp(-t * 47) * 0.76;
    const branches = (mid - low) * Math.sin(t * 211) ** 6 * Math.exp(-t * 14) * 0.8;
    const tail = (low * 1.4 + 0.14 * Math.sin(2 * Math.PI * (103 * t - 97 * t * t))) * Math.exp(-t * 12);
    const sample = Math.tanh((snap + branches + tail) * envelope);
    pcm.writeInt16LE(Math.round(sample * 29000), i * 2);
}

const output = fileURLToPath(new URL(
    '../src/main/resources/assets/narutomod/sounds/jutsu/gian_crack.ogg', import.meta.url,
));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode Gian: ${result.status}`);
}
