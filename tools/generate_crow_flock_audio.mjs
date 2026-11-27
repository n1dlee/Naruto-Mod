import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const duration = 0.68;
const frames = Math.round(duration * rate);
const pcm = Buffer.alloc(frames * 2);
const wingbeats = [[0, 1], [0.071, 0.78], [0.157, 0.70], [0.286, 0.39], [0.381, 0.20]];
let seed = 73711;
let body = 0;
let low = 0;
for (let i = 0; i < frames; i++) {
    const t = i / rate;
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    const noise = seed / 2147483648 - 1;
    low += (noise - low) * 0.016;
    body += (noise - body) * 0.19;
    let lift = 0;
    for (const [offset, gain] of wingbeats) {
        const phase = t - offset;
        if (phase > 0) {
            lift += Math.min(1, phase / 0.018) * gain * Math.exp(-phase * 25);
        }
    }
    const flap = ((body - low) * 0.68 + low * 1.44) * lift;
    const fade = Math.min(1, t / 0.024, (duration - t) / 0.12);
    pcm.writeInt16LE(Math.round(Math.tanh(flap * fade) * 29100), i * 2);
}

const output = fileURLToPath(new URL('../src/main/resources/assets/narutomod/sounds/jutsu/crow_flock_wings.ogg', import.meta.url));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode crow wings: ${result.status}`);
}
