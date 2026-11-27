import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
for (const [name, duration, root, bloom] of [['open', 0.43, 294, 37], ['heal', 0.31, 392, -17]]) {
    const pcm = Buffer.alloc(Math.round(rate * duration) * 2);
    let seed = 5312;
    let soft = 0;
    for (let i = 0; i < pcm.length / 2; i++) {
        const t = i / rate;
        seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
        soft += (seed / 2147483648 - 1 - soft) * 0.04;
        const phase = 2 * Math.PI * t * (root + bloom * t);
        const breath = soft * 0.38;
        const chord = Math.sin(phase) * 0.12 + Math.sin(phase * 1.495) * 0.09 + Math.sin(phase * 2.006) * 0.027;
        const p = t / duration;
        const env = Math.pow(Math.sin(Math.PI * p), 1.56) * (0.94 + 0.06 * Math.sin(t * 43));
        pcm.writeInt16LE(Math.round(Math.tanh((chord + breath) * env) * 29100), i * 2);
    }
    const output = fileURLToPath(new URL(`../src/main/resources/assets/narutomod/sounds/jutsu/medical_palm_${name}.ogg`, import.meta.url));
    const result = spawnSync('ffmpeg', [
        '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
        '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
    ], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(`FFmpeg could not encode Medical Palm ${name}: ${result.status}`);
    }
}
