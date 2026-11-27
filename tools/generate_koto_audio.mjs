import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const duration = 0.48;
const frames = Math.round(rate * duration);
const pcm = Buffer.alloc(frames * 2);
let seed = 63211;
let breath = 0;
let base = 0;
for (let i = 0; i < frames; i++) {
    const t = i / rate;
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    const noise = seed / 2147483648 - 1;
    breath += (noise - breath) * 0.13;
    base += (noise - base) * 0.017;
    const contour = Math.sin(Math.PI * t / duration) ** 2;
    const undertone = Math.sin(Math.PI * 2 * (199 * t - 26 * t * t)) * 0.12
        + Math.sin(Math.PI * 2 * (307 * t - 53 * t * t)) * 0.05;
    const whisper = (breath - base) * (0.79 - t) + undertone * Math.exp(-4 * t);
    pcm.writeInt16LE(Math.round(Math.tanh(whisper * contour) * 28600), i * 2);
}

const output = fileURLToPath(new URL('../src/main/resources/assets/narutomod/sounds/jutsu/koto_whisper.ogg', import.meta.url));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode Kotoamatsukami: ${result.status}`);
}
