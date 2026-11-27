import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const duration = 0.51;
const pcm = Buffer.alloc(Math.round(rate * duration) * 2);
let seed = 43131;
let rasp = 0;
let bass = 0;
for (let i = 0; i < pcm.length / 2; i++) {
    const t = i / rate;
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    const noise = seed / 2147483648 - 1;
    rasp += (noise - rasp) * (0.21 - 0.15 * t);
    bass += (noise - bass) * 0.029;
    const contour = Math.sin(Math.PI * t / duration) ** 1.25;
    const scuff = (rasp - bass) * (0.83 + 0.17 * Math.cos(49 * t + 30 * t * t));
    const grain = Math.sin(2 * Math.PI * (82 * t - 23 * t * t)) * 0.16 * Math.exp(-4.2 * t);
    pcm.writeInt16LE(Math.round(Math.tanh((scuff * 1.24 + bass * 0.89 + grain) * contour) * 29400), i * 2);
}

const output = fileURLToPath(new URL('../src/main/resources/assets/narutomod/sounds/jutsu/shadow_weave.ogg', import.meta.url));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode shadow weave: ${result.status}`);
}
