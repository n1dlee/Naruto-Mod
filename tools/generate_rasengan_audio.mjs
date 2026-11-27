import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const sampleRate = 44100;
const profiles = [
    { name: 'entity', duration: 0.43, seed: 401, weight: 0.67, crack: 0.28, air: 0.27 },
    { name: 'block', duration: 0.37, seed: 733, weight: 0.48, crack: 0.63, air: 0.15 },
];

function synthesize(profile) {
    const samples = Buffer.alloc(Math.ceil(profile.duration * sampleRate) * 2);
    let seed = profile.seed;
    let bodyPhase = 0;
    let airPhase = 0;
    let lowNoise = 0;
    let previousNoise = 0;
    for (let offset = 0; offset < samples.length; offset += 2) {
        const time = offset / (sampleRate * 2);
        seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
        const noise = seed / 2147483648 - 1;
        lowNoise += (noise - lowNoise) * 0.15;

        bodyPhase += 2 * Math.PI * (58 + 108 * Math.exp(-time * 49)) / sampleRate;
        airPhase += 2 * Math.PI * (440 + 920 * Math.exp(-time * 11)) / sampleRate;
        const body = (Math.sin(bodyPhase) + 0.24 * Math.sin(bodyPhase * 2.02))
            * Math.exp(-time * 19) * profile.weight;
        const crack = (noise - previousNoise * 0.65) * Math.exp(-time * 112) * profile.crack;
        const air = (0.27 * Math.sin(airPhase) + lowNoise) * Math.exp(-time * 11)
            * (0.68 + 0.32 * Math.cos(time * 122)) * profile.air;
        const envelope = Math.min(1, time / 0.0025, (profile.duration - time) / 0.025);
        const value = Math.tanh((body + crack + air) * envelope) * 0.92;
        samples.writeInt16LE(Math.round(value * 32767), offset);
        previousNoise = noise;
    }
    return samples;
}

for (const profile of profiles) {
    const output = fileURLToPath(new URL(
        `../src/main/resources/assets/narutomod/sounds/jutsu/rasengan_hit_${profile.name}.ogg`,
        import.meta.url,
    ));
    const result = spawnSync('ffmpeg', [
        '-hide_banner', '-loglevel', 'error', '-y',
        '-f', 's16le', '-ar', String(sampleRate), '-ac', '1', '-i', 'pipe:0',
        '-c:a', 'libvorbis', '-q:a', '5', output,
    ], { input: synthesize(profile), stdio: ['pipe', 'inherit', 'inherit'] });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(`FFmpeg could not encode ${profile.name}: ${result.status}`);
    }
}
