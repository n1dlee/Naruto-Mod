import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const duration = 0.78;
const frames = Math.round(rate * duration);
const pcm = Buffer.alloc(frames * 2);
let seed = 8019;
let low = 0;
let air = 0;
for (let i = 0; i < frames; i++) {
    const t = i / rate;
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    const white = seed / 2147483648 - 1;
    low += (white - low) * 0.025;
    air += (white - air) * 0.09;
    const breathing = (air - low) * (0.64 + 0.31 * Math.cos(2 * Math.PI * 5.2 * t));
    const resonance = Math.sin(2 * Math.PI * (257 * t - 38 * t * t)) * 0.10
        + Math.sin(2 * Math.PI * (131 * t - 13 * t * t)) * 0.17;
    const attack = Math.min(1, t / 0.072, (duration - t) / 0.21);
    const sample = Math.tanh((breathing + resonance * Math.exp(-3.9 * t) + 0.71 * low) * attack);
    pcm.writeInt16LE(Math.round(sample * 28500), i * 2);
}

const output = fileURLToPath(new URL('../src/main/resources/assets/narutomod/sounds/jutsu/sharingan_whisper.ogg', import.meta.url));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode Sharingan whisper: ${result.status}`);
}
