import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const tracks = [
    {
        id: 'breath', duration: 1.40,
        sample(t, progress) {
            const phase = Math.PI * 2 * progress;
            const air = Math.sin(phase * 129 + Math.sin(phase * 5))
                + 0.45 * Math.sin(phase * 197 + Math.sin(phase * 7))
                + 0.18 * Math.sin(phase * 314 + Math.sin(phase * 11));
            const envelope = 0.18 + 0.82 * Math.pow(Math.sin(phase * 0.5), 2);
            return air * envelope * 0.13;
        },
    },
    {
        id: 'full', duration: 0.42,
        sample(t) {
            const swell = Math.min(1, t / 0.014, (0.42 - t) / 0.065);
            const body = 0.51 * Math.sin(2 * Math.PI * 523.25 * t) * Math.exp(-t * 7.8);
            const shimmer = 0.22 * Math.sin(2 * Math.PI * 784 * t) * Math.exp(-t * 9.6);
            return (body + shimmer) * swell;
        },
    },
];

for (const track of tracks) {
    const frames = Math.round(rate * track.duration);
    const pcm = Buffer.alloc(frames * 2);
    for (let i = 0; i < frames; i++) {
        pcm.writeInt16LE(Math.round(track.sample(i / rate, i / frames) * 29000), i * 2);
    }
    const path = fileURLToPath(new URL(
        `../src/main/resources/assets/narutomod/sounds/jutsu/chakra_charge_${track.id}.ogg`, import.meta.url,
    ));
    const result = spawnSync('ffmpeg', [
        '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
        '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', path,
    ], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(`FFmpeg could not encode ${track.id}: ${result.status}`);
    }
}
