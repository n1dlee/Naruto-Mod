import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const duration = 0.31;
const pcm = Buffer.alloc(Math.round(rate * duration) * 2);
let seed = 8203;
let texture = 0;
for (let i = 0; i < pcm.length / 2; i++) {
    const t = i / rate;
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    const noise = seed / 2147483648 - 1;
    texture += (noise - texture) * 0.26;
    const cord = Math.sin(2 * Math.PI * (263 * t - 216 * t * t)) * Math.exp(-t * 16) * 0.34;
    const core = Math.sin(2 * Math.PI * (115 * t - 72 * t * t)) * Math.exp(-t * 11) * 0.27;
    const flick = (noise - texture) * Math.exp(-t * 52) * 0.33 + texture * Math.exp(-t * 23) * 0.69;
    const envelope = Math.min(1, t / 0.007, (duration - t) / 0.07);
    pcm.writeInt16LE(Math.round(Math.tanh((cord + core + flick) * envelope) * 29600), i * 2);
}

const output = fileURLToPath(new URL('../src/main/resources/assets/narutomod/sounds/jutsu/shadow_needle.ogg', import.meta.url));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode shadow needle: ${result.status}`);
}
