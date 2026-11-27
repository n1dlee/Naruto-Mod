import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const cues = [
    { id: 'flight', length: 0.62, seed: 181, rise: 0.065, fall: 5, wind: 0.40, tone: 0.53 },
    { id: 'bloom', length: 0.85, seed: 819, rise: 0.03, fall: 3.8, wind: 0.94, tone: 0.28 },
];

function samples(cue) {
    const output = Buffer.alloc(Math.round(rate * cue.length) * 2);
    let seed = cue.seed;
    let band = 0;
    let low = 0;
    let phase = 0;
    for (let i = 0; i < output.length / 2; i++) {
        const t = i / rate;
        seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
        const noise = seed / 2147483648 - 1;
        low += 0.045 * (noise - low);
        band += 0.23 * (noise - band);
        phase += 2 * Math.PI * (940 + 1150 * Math.exp(-t * 6.3)) / rate;
        const mod = 0.66 + 0.34 * Math.sin(t * (300 - t * 160));
        const wind = (band - low * 0.75 + low * 0.46 * Math.exp(-t * 10)) * cue.wind * mod;
        const whistle = Math.sin(phase + 0.22 * Math.sin(t * 172)) * cue.tone;
        const attack = Math.min(1, t / cue.rise);
        const tail = Math.max(0, Math.min(1, (cue.length - t) / 0.12));
        const value = Math.tanh((wind + whistle) * attack * Math.exp(-t * cue.fall) * 1.6) * tail;
        output.writeInt16LE(Math.round(value * 30000), i * 2);
    }
    return output;
}

for (const cue of cues) {
    const path = fileURLToPath(new URL(
        `../src/main/resources/assets/narutomod/sounds/jutsu/rasenshuriken_${cue.id}.ogg`, import.meta.url,
    ));
    const result = spawnSync('ffmpeg', [
        '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
        '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', path,
    ], { input: samples(cue), stdio: ['pipe', 'inherit', 'inherit'] });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(`FFmpeg could not encode ${cue.id}: ${result.status}`);
    }
}
