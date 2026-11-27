import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const sampleRate = 44100;
const cues = [
    { id: 'form', duration: 0.45, seed: 718, attack: 0.15, fall: 1.1, body: 0.62, foam: 0.35, wet: 0.54, chirp: 1 },
    { id: 'launch', duration: 0.49, seed: 428, attack: 0.025, fall: 7, body: 0.84, foam: 0.68, wet: 0.32, chirp: -1 },
    { id: 'impact', duration: 0.62, seed: 317, attack: 0.007, fall: 9, body: 0.91, foam: 0.92, wet: 0.73, chirp: -1 },
];

function createSamples(cue) {
    const samples = Buffer.alloc(Math.ceil(cue.duration * sampleRate) * 2);
    let seed = cue.seed;
    let low = 0;
    let high = 0;
    let dropPhase = 0;
    let swell = 0;
    for (let i = 0; i < samples.length / 2; i++) {
        const t = i / sampleRate;
        const progress = t / cue.duration;
        seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
        const noise = seed / 2147483648 - 1;
        low += 0.045 * (noise - low);
        high += (0.15 + progress * 0.22) * (noise - high);
        swell += 2 * Math.PI * (91 + cue.chirp * 50 * progress) / sampleRate;
        dropPhase += 2 * Math.PI * (490 + cue.chirp * 160 * progress + low * 130) / sampleRate;
        const splatter = Math.pow(0.5 + 0.5 * Math.sin(t * 219 + low * 8), 4);
        const burst = Math.min(1, t / cue.attack) * Math.exp(-t * cue.fall);
        const body = (low * 1.9 + Math.sin(swell) * 0.22) * cue.body;
        const foam = (high - low * 0.75) * cue.foam * (0.6 + splatter * 0.9);
        const drops = (Math.sin(dropPhase) + 0.21 * Math.sin(dropPhase * 1.61)) * splatter
            * Math.exp(-t * 3.6) * cue.wet;
        const tail = Math.max(0, Math.min(1, (cue.duration - t) / 0.055));
        const sample = Math.tanh(((body + foam) * burst + drops * Math.min(1, t / 0.018)) * 1.4) * tail;
        samples.writeInt16LE(Math.round(sample * 30000), i * 2);
    }
    return samples;
}

for (const cue of cues) {
    const path = fileURLToPath(new URL(
        `../src/main/resources/assets/narutomod/sounds/jutsu/water_dragon_${cue.id}.ogg`, import.meta.url,
    ));
    const result = spawnSync('ffmpeg', [
        '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(sampleRate),
        '-ac', '1', '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', path,
    ], { input: createSamples(cue), stdio: ['pipe', 'inherit', 'inherit'] });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(`FFmpeg could not encode ${cue.id}: ${result.status}`);
    }
}
