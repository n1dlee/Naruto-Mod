import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
for (const [name, duration, root, decay, attack] of [
    ['tuck', 0.33, 138, 3.3, 0.068],
    ['roll', 0.53, 63, 3.7, 0.024],
    ['bump', 0.30, 79, 13, 0.005],
]) {
    const pcm = Buffer.alloc(Math.round(rate * duration) * 2);
    let seed = 82479;
    let loose = 0;
    let ground = 0;
    for (let i = 0; i < pcm.length / 2; i++) {
        const t = i / rate;
        seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
        const noise = seed / 2147483648 - 1;
        loose += (noise - loose) * 0.18;
        ground += (noise - ground) * 0.024;
        const friction = 0.67 + 0.33 * Math.sin(t * 2 * Math.PI * (28 - 10 * t));
        const thud = Math.sin(t * 2 * Math.PI * (root - 26 * t)) * (name === 'bump' ? 0.65 : 0.39);
        const env = Math.min(1, t / attack, (duration - t) / 0.09) * Math.exp(-decay * t);
        const sample = (ground * 1.32 + (loose - ground) * 0.70 + thud) * env * friction;
        pcm.writeInt16LE(Math.round(Math.tanh(sample) * 30000), i * 2);
    }
    const output = fileURLToPath(new URL(`../src/main/resources/assets/narutomod/sounds/jutsu/boulder_${name}.ogg`, import.meta.url));
    const result = spawnSync('ffmpeg', [
        '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
        '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
    ], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(`FFmpeg could not encode Human Boulder ${name}: ${result.status}`);
    }
}
