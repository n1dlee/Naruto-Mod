import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const duration = 1.14;
const length = Math.round(rate * duration);
const pcm = Buffer.alloc(length * 2);
let seed = 301151;
let low = 0;
let mid = 0;
for (let i = 0; i < length; i++) {
    const time = i / rate;
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    const noise = seed / 2147483648 - 1;
    low += (noise - low) * 0.042;
    mid += (noise - mid) * 0.25;
    const pulse = Math.floor((time - 0.2) / 0.15);
    const shotTime = time - 0.2 - pulse * 0.15;
    const breath = time < 0.2 ? Math.sin(Math.PI * time / 0.2) * (mid - low * 0.46) * 0.53 : 0;
    const strength = 0.76 - Math.abs(pulse - 2.2) * 0.07;
    const tail = pulse >= 0 && pulse < 6
        ? Math.min(1, shotTime / 0.016) * Math.exp(-shotTime * 22) * strength
        : 0;
    const fire = ((mid - low) * 1.21 + low * 1.43) * tail;
    const ember = time > 0.17 ? Math.sin(time * 307) ** 8 * noise * Math.max(0, (1.13 - time) * 0.094) : 0;
    const sample = Math.tanh((breath + fire + ember) * 1.6);
    pcm.writeInt16LE(Math.round(sample * 29000), i * 2);
}

const path = fileURLToPath(new URL(
    '../src/main/resources/assets/narutomod/sounds/jutsu/phoenix_flower_volley.ogg', import.meta.url,
));
const result = spawnSync('ffmpeg', [
    '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
    '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', path,
], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
if (result.error) {
    throw result.error;
}
if (result.status !== 0) {
    throw new Error(`FFmpeg could not encode Phoenix Flower: ${result.status}`);
}
