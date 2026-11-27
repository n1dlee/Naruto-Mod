import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const duration = 0.69;
const frames = Math.round(rate * duration);
const pcm = Buffer.alloc(frames * 2);
let seed = 1442087;
let rubble = 0;
let rumble = 0;
for (let i = 0; i < frames; i++) {
    const time = i / rate;
    const phase = Math.max(0, time - 0.08);
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    const noise = seed / 2147483648 - 1;
    rubble += (noise - rubble) * 0.36;
    rumble += (noise - rumble) * 0.046;
    const impulse = Math.exp(-Math.min(10, Math.abs(time - 0.09) * 52));
    const stones = Math.sin(132 * phase) ** 8 * Math.exp(-phase * 3.8);
    const tick = (noise * 0.38 + rubble * 0.49) * impulse;
    const bass = (rumble * 1.8 + Math.sin(2 * Math.PI * (87 * phase - 38 * phase * phase)) * 0.12) * stones;
    const value = Math.tanh((tick + bass) * Math.min(1, time / 0.011, (duration - time) / 0.08));
    pcm.writeInt16LE(Math.round(value * 28000), i * 2);
}

const output = fileURLToPath(new URL(
    '../src/main/resources/assets/narutomod/sounds/jutsu/earth_spikes_crack.ogg', import.meta.url,
));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode Earth Spikes: ${result.status}`);
}
