import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const profiles = [
    { tag: 'lift', seed: 27311, duration: 0.41, start: 106, glide: 124, grit: 0.39, decay: 2.0 },
    { tag: 'clench', seed: 6513, duration: 0.25, start: 108, glide: -165, grit: 0.31, decay: 10.0 },
    { tag: 'drop', seed: 12071, duration: 0.35, start: 180, glide: -219, grit: 0.62, decay: 4.2 },
];

for (const p of profiles) {
    const pcm = Buffer.alloc(Math.round(rate * p.duration) * 2);
    let seed = p.seed;
    let low = 0;
    let fibre = 0;
    for (let i = 0; i < pcm.length / 2; i++) {
        const t = i / rate;
        seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
        const white = seed / 2147483648 - 1;
        low += (white - low) * 0.021;
        fibre += (white - fibre) * 0.14;
        const wrap = Math.sin(2 * Math.PI * (p.start * t + p.glide * t * t * 0.5)) * 0.27;
        const texture = (fibre - low) * p.grit * (0.91 + 0.09 * Math.sin(t * 143));
        const env = Math.min(1, t / 0.016, (p.duration - t) / 0.081);
        const sample = (low * 0.97 + texture + wrap) * env * Math.exp(-t * p.decay);
        pcm.writeInt16LE(Math.round(Math.tanh(sample) * 29500), i * 2);
    }
    const output = fileURLToPath(new URL(`../src/main/resources/assets/narutomod/sounds/jutsu/shadow_grip_${p.tag}.ogg`, import.meta.url));
    const result = spawnSync('ffmpeg', [
        '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
        '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
    ], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(`FFmpeg could not encode Shadow Grip ${p.tag}: ${result.status}`);
    }
}
