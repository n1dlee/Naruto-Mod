import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
for (const [name, duration, impact] of [['spin', 0.69, false], ['contact', 0.19, true]]) {
    const pcm = Buffer.alloc(Math.round(rate * duration) * 2);
    let seed = 81299;
    let surge = 0;
    let gust = 0;
    for (let i = 0; i < pcm.length / 2; i++) {
        const t = i / rate;
        seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
        const white = seed / 2147483648 - 1;
        surge += (white - surge) * 0.065;
        gust += (white - gust) * 0.21;
        const spin = 0.56 + 0.44 * Math.sin(2 * Math.PI * t * (8 + 4 * t));
        const attack = impact ? 0.003 : 0.12;
        const tone = impact ? 0.37 * Math.sin(t * 2 * Math.PI * (880 - 1140 * t))
            : 0.13 * Math.sin(t * 2 * Math.PI * (164 + 45 * t));
        const env = Math.min(1, t / attack, (duration - t) / 0.083) * Math.exp(-t * (impact ? 24 : 1.3));
        const sample = Math.tanh(((gust - surge) * 1.34 * spin + surge * 1.03 + tone) * env);
        pcm.writeInt16LE(Math.round(sample * 30600), i * 2);
    }
    const output = fileURLToPath(new URL(`../src/main/resources/assets/narutomod/sounds/jutsu/kaiten_${name}.ogg`, import.meta.url));
    const result = spawnSync('ffmpeg', [
        '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
        '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
    ], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(`FFmpeg could not encode Kaiten ${name}: ${result.status}`);
    }
}
