import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const cues = [
    { id: 'exhale', time: 0.30, seed: 421, bass: 0.9, hiss: 0.26 },
    { id: 'steam', time: 0.51, seed: 715, bass: 0.12, hiss: 0.93 },
];

function pcm(cue) {
    const frames = Math.floor(cue.time * rate);
    const result = Buffer.alloc(frames * 2);
    let seed = cue.seed;
    let low = 0;
    let band = 0;
    let phase = 0;
    for (let i = 0; i < frames; i++) {
        const t = i / rate;
        seed = (Math.imul(1664525, seed) + 1013904223) >>> 0;
        const noise = seed / 2147483648 - 1;
        low += (noise - low) * 0.062;
        band += (noise - band) * 0.46;
        phase += 2 * Math.PI * (84 + 100 * Math.exp(-t * 34)) / rate;
        const body = (low * 1.8 + Math.sin(phase) * 0.26) * Math.exp(-t * 13) * cue.bass;
        const hiss = (band - low * 0.85) * (0.84 + Math.sin(t * 202) * 0.16) * Math.exp(-t * 4) * cue.hiss;
        const envelope = Math.min(1, t / 0.017, (cue.time - t) / 0.05);
        result.writeInt16LE(Math.round(Math.tanh((body + hiss) * envelope * 1.62) * 30000), i * 2);
    }
    return result;
}

for (const cue of cues) {
    const path = fileURLToPath(new URL(
        `../src/main/resources/assets/narutomod/sounds/jutsu/great_fireball_${cue.id}.ogg`, import.meta.url,
    ));
    const result = spawnSync('ffmpeg', [
        '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
        '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', path,
    ], { input: pcm(cue), stdio: ['pipe', 'inherit', 'inherit'] });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(`FFmpeg could not encode ${cue.id}: ${result.status}`);
    }
}
