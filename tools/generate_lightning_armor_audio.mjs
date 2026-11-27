import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const frames = Math.round(rate * 1.25);
const pcm = Buffer.alloc(frames * 2);
for (let i = 0; i < frames; i++) {
    const phase = 2 * Math.PI * i / frames;
    const flutter = Math.sin(phase * 11) * 0.24 + Math.sin(phase * 17) * 0.09;
    const mains = Math.sin(phase * 94 + flutter) * 0.37;
    const coil = Math.sin(phase * 141 + Math.sin(phase * 7) * 0.71) * 0.12;
    const fizz = Math.sin(phase * 409 + Math.sin(phase * 19) * 1.3) * 0.07;
    const t = 0.67 + 0.20 * Math.sin(phase * 3) + 0.10 * Math.cos(phase * 5);
    pcm.writeInt16LE(Math.round((mains + coil + fizz) * t * 23400), i * 2);
}

const output = fileURLToPath(new URL(
    '../src/main/resources/assets/narutomod/sounds/jutsu/lightning_armor_hum.ogg', import.meta.url,
));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode Lightning Armor: ${result.status}`);
}
