import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const beats = [
    ['field', 0.37, [0.025, 0.12], 540],
    ['two', 0.18, [0.008, 0.077], 125],
    ['four', 0.17, [0.006, 0.057], 138],
    ['eight', 0.19, [0.006, 0.038, 0.071, 0.104], 152],
    ['sixteen', 0.20, [0.006, 0.023, 0.041, 0.061, 0.084, 0.11], 165],
    ['thirty_two', 0.21, [0.004, 0.016, 0.031, 0.043, 0.057, 0.072, 0.087, 0.109], 180],
    ['seal', 0.39, [0.003, 0.013, 0.026, 0.048, 0.07, 0.093, 0.13, 0.174], 220],
];

for (const [name, duration, strikes, frequency] of beats) {
    const pcm = Buffer.alloc(Math.round(rate * duration) * 2);
    let seed = 12097;
    let body = 0;
    for (let i = 0; i < pcm.length / 2; i++) {
        const t = i / rate;
        seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
        const noise = seed / 2147483648 - 1;
        body += (noise - body) * 0.12;
        let sample = 0;
        for (let k = 0; k < strikes.length; k++) {
            const dt = t - strikes[k];
            if (dt <= 0) {
                break;
            }
            const pitch = frequency * (1 + 0.13 * (k % 3));
            const air = (noise - body) * Math.exp(-dt * 174) * 0.35;
            const membrane = Math.sin(2 * Math.PI * pitch * dt) * Math.exp(-dt * (frequency > 250 ? 16 : 69)) * 0.29;
            sample += (air + membrane) * Math.min(1, dt / 0.003) / (1 + k * 0.11);
        }
        const seal = name === 'seal' && t > 0.174
            ? Math.sin((t - 0.174) * 2 * Math.PI * 739) * Math.exp(-(t - 0.174) * 18) * Math.min(1, (t - 0.174) / 0.006) * 0.22
            : 0;
        const out = Math.tanh((sample + seal) * Math.min(1, (duration - t) / 0.046));
        pcm.writeInt16LE(Math.round(out * 31400), i * 2);
    }
    const output = fileURLToPath(new URL(`../src/main/resources/assets/narutomod/sounds/jutsu/trigram_${name}.ogg`, import.meta.url));
    const result = spawnSync('ffmpeg', [
        '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
        '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
    ], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(`FFmpeg could not encode Trigram Palms ${name}: ${result.status}`);
    }
}
