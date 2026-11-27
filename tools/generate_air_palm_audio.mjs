import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
for (const [name, duration, attack, frequency, sweep, weight, decay] of [
    ['release', 0.29, 0.014, 392, 1230, 0.24, 12],
    ['hit', 0.21, 0.004, 184, 680, 0.49, 24],
]) {
    const pcm = Buffer.alloc(Math.round(rate * duration) * 2);
    let seed = 37213;
    let air = 0;
    let bass = 0;
    for (let i = 0; i < pcm.length / 2; i++) {
        const t = i / rate;
        seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
        const noise = seed / 2147483648 - 1;
        air += (noise - air) * 0.23;
        bass += (noise - bass) * 0.037;
        const pitch = 2 * Math.PI * t * (frequency - sweep * t * 0.5);
        const env = Math.min(1, t / attack, (duration - t) / 0.065) * Math.exp(-decay * t);
        const sample = Math.tanh(((air - bass) * 1.55 + bass * 0.94 + Math.sin(pitch) * weight) * env);
        pcm.writeInt16LE(Math.round(sample * 30000), i * 2);
    }
    const output = fileURLToPath(new URL(`../src/main/resources/assets/narutomod/sounds/jutsu/air_palm_${name}.ogg`, import.meta.url));
    const result = spawnSync('ffmpeg', [
        '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
        '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
    ], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(`FFmpeg could not encode Air Palm ${name}: ${result.status}`);
    }
}
