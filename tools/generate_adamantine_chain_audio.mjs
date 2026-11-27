import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const profiles = [
    ['spool', 0.45, [0.018, 0.075, 0.141, 0.22, 0.309], 1.01, 0.51],
    ['lock', 0.28, [0.008, 0.041], 0.84, 0.72],
    ['release', 0.43, [0.013, 0.103, 0.206, 0.31], 0.92, 0.39],
];

for (const [name, duration, strikes, pitch, gain] of profiles) {
    const pcm = Buffer.alloc(Math.round(rate * duration) * 2);
    let seed = 92731;
    let body = 0;
    for (let i = 0; i < pcm.length / 2; i++) {
        const t = i / rate;
        seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
        const white = seed / 2147483648 - 1;
        body += (white - body) * 0.18;
        let sum = 0;
        for (let k = 0; k < strikes.length; k++) {
            const dt = t - strikes[k];
            if (dt < 0) {
                break;
            }
            const frequency = pitch * (1 + 0.043 * k);
            const ring = 0.21 * Math.sin(dt * frequency * 2 * Math.PI * 559) * Math.exp(-dt * 42)
                + 0.12 * Math.sin(dt * frequency * 2 * Math.PI * 1613) * Math.exp(-dt * 55)
                + 0.09 * Math.sin(dt * frequency * 2 * Math.PI * 2879) * Math.exp(-dt * 83);
            const clack = (white * 0.34 + body * 0.63) * Math.exp(-dt * 132);
            sum += (ring + clack) * Math.min(1, dt / 0.0015) / (1 + 0.24 * k);
        }
        const sample = Math.tanh(sum * gain * Math.min(1, (duration - t) / 0.039));
        pcm.writeInt16LE(Math.round(sample * 32000), i * 2);
    }
    const output = fileURLToPath(new URL(`../src/main/resources/assets/narutomod/sounds/jutsu/adamantine_${name}.ogg`, import.meta.url));
    const result = spawnSync('ffmpeg', [
        '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
        '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
    ], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(`FFmpeg could not encode Adamantine Chains ${name}: ${result.status}`);
    }
}
