import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const duration = 0.41;
const samples = Math.round(duration * rate);
const pcm = Buffer.alloc(samples * 2);
let seed = 89101;
let bass = 0;
let breath = 0;
for (let i = 0; i < samples; i++) {
    const t = i / rate;
    const progress = t / duration;
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    const noise = seed / 2147483648 - 1;
    bass += (noise - bass) * 0.023;
    breath += (noise - breath) * (0.31 - 0.25 * progress);
    const air = (breath - bass) * Math.sin(Math.PI * progress) ** 2;
    const spiral = Math.sin(2 * Math.PI * (134 * t - 100 * t * t)) * 0.25;
    const rumble = (spiral + bass * 0.67) * Math.exp(-((progress - 0.67) ** 2) * 18);
    const envelope = Math.min(1, t / 0.040, (duration - t) / 0.074);
    pcm.writeInt16LE(Math.round(Math.tanh((air * 1.31 + rumble) * envelope) * 30000), i * 2);
}

const output = fileURLToPath(new URL('../src/main/resources/assets/narutomod/sounds/jutsu/kamui_fold.ogg', import.meta.url));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode Kamui: ${result.status}`);
}
