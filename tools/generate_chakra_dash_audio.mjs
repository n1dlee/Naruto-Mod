import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const duration = 0.25;
const pcm = Buffer.alloc(Math.round(rate * duration) * 2);
let seed = 517;
let low = 0;
let mid = 0;
for (let i = 0; i < pcm.length / 2; i++) {
    const time = i / rate;
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    const noise = seed / 2147483648 - 1;
    low += (noise - low) * 0.04;
    mid += (noise - mid) * (0.29 - time * 0.67);
    const envelope = Math.min(1, time / 0.022, (duration - time) / 0.055) * Math.exp(-time * 5);
    const value = Math.tanh(((mid - low * 0.48) * 1.51 + low * 1.36) * envelope);
    pcm.writeInt16LE(Math.round(value * 30000), i * 2);
}

const path = fileURLToPath(new URL(
    '../src/main/resources/assets/narutomod/sounds/jutsu/chakra_dash_wind.ogg', import.meta.url,
));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', path,
], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode Chakra Dash: ${result.status}`);
}
