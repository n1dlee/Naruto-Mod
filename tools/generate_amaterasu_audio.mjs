import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const duration = 0.72;
const frames = Math.round(duration * rate);
const pcm = Buffer.alloc(frames * 2);
let seed = 52733;
let low = 0;
let jaw = 0;
for (let i = 0; i < frames; i++) {
    const t = i / rate;
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    const noise = seed / 2147483648 - 1;
    low += (noise - low) * 0.028;
    jaw += (noise - jaw) * 0.105;
    const char = jaw * 0.95 + low * 1.31;
    const onset = Math.min(1, t / 0.022, (duration - t) / 0.14);
    const drop = Math.sin(2 * Math.PI * (98 * t - 49 * t * t)) * 0.27 * Math.exp(-7 * t);
    const fold = Math.sin(183 * t) ** 6 * Math.exp(-t * 4) * (jaw - low) * 0.60;
    const sound = Math.tanh((char * Math.exp(-t * 3.4) + drop + fold) * onset);
    pcm.writeInt16LE(Math.round(sound * 29900), i * 2);
}

const output = fileURLToPath(new URL(
    '../src/main/resources/assets/narutomod/sounds/jutsu/amaterasu_ignite.ogg', import.meta.url,
));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode Amaterasu: ${result.status}`);
}
