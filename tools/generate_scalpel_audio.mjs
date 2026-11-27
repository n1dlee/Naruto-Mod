import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
for (const [name, duration, start, sweep, attack] of [
    ['form', 0.27, 480, 550, 0.042],
    ['cut', 0.18, 712, -1490, 0.003],
]) {
    const pcm = Buffer.alloc(Math.round(rate * duration) * 2);
    let seed = 90023;
    let taper = 0;
    for (let i = 0; i < pcm.length / 2; i++) {
        const t = i / rate;
        seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
        const noise = seed / 2147483648 - 1;
        taper += (noise - taper) * 0.27;
        const phase = 2 * Math.PI * t * (start + sweep * t);
        const edge = (noise - taper) * (name === 'cut' ? 0.36 : 0.17);
        const sheen = Math.sin(phase) * 0.20 + Math.sin(phase * 1.98) * 0.045;
        const env = Math.min(1, t / attack, (duration - t) / 0.053) * Math.exp(-t * (name === 'cut' ? 19 : 4));
        pcm.writeInt16LE(Math.round(Math.tanh((edge + sheen + taper * 0.19) * env) * 30600), i * 2);
    }
    const output = fileURLToPath(new URL(`../src/main/resources/assets/narutomod/sounds/jutsu/scalpel_${name}.ogg`, import.meta.url));
    const result = spawnSync('ffmpeg', [
        '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
        '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
    ], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(`FFmpeg could not encode Chakra Scalpel ${name}: ${result.status}`);
    }
}
