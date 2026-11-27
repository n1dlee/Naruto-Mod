import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const duration = 1.58;
const frames = Math.round(rate * duration);
const pcm = Buffer.alloc(frames * 2);
let seed = 7253;
let low = 0;
for (let i = 0; i < frames; i++) {
    const t = i / rate;
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    const noise = seed / 2147483648 - 1;
    low += (noise - low) * 0.035;
    let echo = 0;
    for (const [offset, gain, tone] of [[0.014, 1, 146], [0.283, 0.50, 131], [0.584, 0.25, 109]]) {
        const phase = t - offset;
        if (phase > 0) {
            const envelope = Math.min(1, phase / 0.032) * Math.exp(-phase * 6.1);
            const bell = Math.sin(2 * Math.PI * (tone * phase - 15 * phase ** 2)) * 0.48;
            const undertone = Math.sin(Math.PI * tone * phase) * 0.21;
            echo += (bell + undertone + low * 0.65) * gain * envelope;
        }
    }
    const tail = Math.min(1, (duration - t) / 0.19);
    pcm.writeInt16LE(Math.round(Math.tanh(echo * tail) * 29000), i * 2);
}

const output = fileURLToPath(new URL('../src/main/resources/assets/narutomod/sounds/jutsu/tsukuyomi_echo.ogg', import.meta.url));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode Tsukuyomi: ${result.status}`);
}
