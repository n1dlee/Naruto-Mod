import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const profiles = [
    { name: 'spirit_open', duration: 0.69, seed: 7231, pitch: 282, drift: 141, breeze: 0.74 },
    { name: 'spirit_return', duration: 0.52, seed: 4217, pitch: 418, drift: -216, breeze: 0.91 },
];
for (const tone of profiles) {
    const pcm = Buffer.alloc(Math.round(rate * tone.duration) * 2);
    let seed = tone.seed;
    let air = 0;
    let swell = 0;
    for (let i = 0; i < pcm.length / 2; i++) {
        const t = i / rate;
        seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
        const noise = seed / 2147483648 - 1;
        air += (noise - air) * 0.19;
        swell += (noise - swell) * 0.021;
        const change = tone.pitch * t + tone.drift * t * t * 0.5;
        const bell = Math.sin(2 * Math.PI * change) * Math.exp(-4 * t) * 0.23;
        const octave = Math.sin(4 * Math.PI * change) * Math.exp(-10 * t) * 0.10;
        const envelope = Math.sin(t / tone.duration * Math.PI) ** 1.3;
        const sample = ((air - swell) * tone.breeze + bell + octave) * envelope;
        pcm.writeInt16LE(Math.round(Math.tanh(sample) * 29200), i * 2);
    }
    const output = fileURLToPath(new URL(`../src/main/resources/assets/narutomod/sounds/jutsu/${tone.name}.ogg`, import.meta.url));
    const result = spawnSync('ffmpeg', [
        '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
        '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
    ], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(`FFmpeg could not encode ${tone.name}: ${result.status}`);
    }
}
