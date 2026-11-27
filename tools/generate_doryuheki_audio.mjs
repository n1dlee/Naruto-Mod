import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const length = Math.round(rate * 0.51);
const pcm = Buffer.alloc(length * 2);
let seed = 92381;
let low = 0;
let grain = 0;
for (let i = 0; i < length; i++) {
    const t = i / rate;
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    const noise = seed / 2147483648 - 1;
    low += (noise - low) * 0.023;
    grain += (noise - grain) * 0.28;
    const crack = Math.sin(t * 112) ** 4 * (grain - low) * 0.92;
    const rumble = low * 1.3 + Math.sin(2 * Math.PI * (61 * t + 18 * t * t)) * 0.16;
    const envelope = Math.min(1, t / 0.026, (0.51 - t) / 0.1) * Math.exp(-1.7 * t);
    pcm.writeInt16LE(Math.round(26000 * Math.tanh((crack + rumble) * envelope)), i * 2);
}

const output = fileURLToPath(new URL(
    '../src/main/resources/assets/narutomod/sounds/jutsu/doryuheki_rise.ogg', import.meta.url,
));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode Doryuheki: ${result.status}`);
}
