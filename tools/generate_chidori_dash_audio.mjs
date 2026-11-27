import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const duration = 0.31;
const output = Buffer.alloc(Math.round(rate * duration) * 2);
let seed = 70129;
let filtered = 0;
let phase = 0;

for (let i = 0; i < output.length / 2; i++) {
    const t = i / rate;
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    const noise = seed / 2147483648 - 1;
    filtered += 0.14 * (noise - filtered);
    phase += 2 * Math.PI * (1640 * Math.exp(-t * 9.2) + 240) / rate;
    const crackle = Math.pow(0.5 + 0.5 * Math.sin(t * 724), 6) * (noise - filtered);
    const chirp = Math.sin(phase + Math.sin(t * 970) * 1.25) * Math.exp(-t * 10);
    const blast = filtered * Math.exp(-t * 19) + crackle * 0.48 * Math.exp(-t * 8);
    const envelope = Math.min(1, t / 0.004, (duration - t) / 0.03);
    output.writeInt16LE(Math.round(Math.tanh((blast + chirp * 0.57) * envelope) * 30000), i * 2);
}

const path = fileURLToPath(new URL(
    '../src/main/resources/assets/narutomod/sounds/jutsu/chidori_dash_launch.ogg', import.meta.url,
));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', path,
], { input: output, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode Chidori Dash: ${result.status}`);
}
