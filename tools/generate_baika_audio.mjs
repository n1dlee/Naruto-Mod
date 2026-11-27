import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
for (const [name, duration, root, sweep, decay, attack] of [
    ['grow', 0.64, 87, -61, 0.8, 0.13],
    ['step', 0.29, 74, -108, 12.0, 0.007],
    ['denied', 0.24, 145, -162, 9.2, 0.009],
]) {
    const pcm = Buffer.alloc(Math.round(rate * duration) * 2);
    let seed = 129709;
    let pressure = 0;
    let grit = 0;
    for (let i = 0; i < pcm.length / 2; i++) {
        const t = i / rate;
        seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
        const white = seed / 2147483648 - 1;
        pressure += (white - pressure) * 0.037;
        grit += (white - grit) * 0.17;
        const phase = 2 * Math.PI * t * (root + sweep * t * 0.5);
        const env = Math.min(1, t / attack, (duration - t) / 0.075) * Math.exp(-decay * t);
        const weight = name === 'grow' ? 0.37 + 0.25 * Math.sin(t * Math.PI / duration) : 0.52;
        const sample = (Math.sin(phase) * weight + pressure * 0.86 + (grit - pressure) * 0.45) * env;
        pcm.writeInt16LE(Math.round(Math.tanh(sample) * 30500), i * 2);
    }
    const output = fileURLToPath(new URL(`../src/main/resources/assets/narutomod/sounds/jutsu/baika_${name}.ogg`, import.meta.url));
    const result = spawnSync('ffmpeg', [
        '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
        '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
    ], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(`FFmpeg could not encode Baika ${name}: ${result.status}`);
    }
}
