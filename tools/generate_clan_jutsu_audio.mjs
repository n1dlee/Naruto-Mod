import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const rate = 44100;
const profiles = {
    shinranshin: [
        ['focus', 0.36, 410, -113, 0.12, 0.19, 2.0],
        ['grip', 0.29, 279, 125, 0.027, 0.14, 3.1],
    ],
    ninken: [
        ['arrive', 0.34, 129, -120, 0.018, 0.71, 6.3],
        ['track', 0.20, 211, -77, 0.034, 0.33, 5.5],
        ['dismiss', 0.31, 156, -154, 0.047, 0.67, 4.1],
    ],
    gatsuga: [
        ['release', 0.64, 121, -85, 0.079, 0.80, 2.4],
        ['stop', 0.23, 88, -143, 0.007, 0.70, 13.3],
    ],
    kikaichu: [
        ['launch', 0.49, 151, -24, 0.091, 0.43, 2.1],
        ['return', 0.26, 201, -60, 0.027, 0.31, 4.9],
    ],
    iron_sand: [
        ['gather', 0.45, 388, -191, 0.14, 0.56, 4.7],
        ['break', 0.34, 233, -329, 0.023, 0.91, 12.5],
    ],
    ice_field: [
        ['warn', 0.34, 846, 432, 0.21, 0.18, 5.0],
        ['rise', 0.23, 1092, -541, 0.014, 0.53, 9.1],
        ['melt', 0.29, 769, -413, 0.018, 0.62, 15.2],
    ],
    laser_circus: [
        ['focus', 0.39, 616, 700, 0.24, 0.14, 3.4],
        ['cast', 0.38, 1390, -960, 0.008, 0.43, 9.5],
    ],
    clay: [
        ['mold', 0.32, 347, -141, 0.043, 0.50, 7.6],
        ['arm', 0.24, 518, 292, 0.016, 0.32, 12.4],
        ['burst', 0.53, 118, -151, 0.006, 0.94, 9.5],
    ],
    shinra: [
        ['pressure', 0.37, 136, -64, 0.27, 0.20, 2.5],
        ['release', 0.62, 109, -104, 0.006, 0.77, 7.7],
        ['wake', 0.41, 204, -157, 0.043, 0.62, 6.4],
    ],
    bansho: [
        ['focus', 0.28, 144, 196, 0.16, 0.31, 4.3],
        ['pull', 0.40, 196, 581, 0.061, 0.54, 2.9],
    ],
    chibaku: [
        ['seed', 0.42, 172, -85, 0.20, 0.22, 3.9],
        ['gather', 0.57, 104, -92, 0.12, 0.63, 6.3],
        ['crash', 0.74, 85, -81, 0.006, 0.91, 14.7],
    ],
    preta: [
        ['open', 0.32, 569, -321, 0.14, 0.18, 3.3],
        ['sip', 0.28, 370, 507, 0.12, 0.49, 5.9],
    ],
    naraka: [
        ['restore', 0.46, 682, 196, 0.22, 0.14, 3.1],
        ['jaw', 0.61, 117, -98, 0.081, 0.78, 5.6],
        ['close', 0.38, 163, -149, 0.011, 0.84, 8.2],
    ],
    summon: [
        ['mark', 0.35, 193, 68, 0.12, 0.35, 5.5],
        ['arrive', 0.49, 134, -101, 0.016, 0.82, 7.8],
        ['ram', 0.48, 101, -45, 0.043, 0.69, 2.9],
        ['toad', 0.51, 93, -72, 0.014, 0.84, 5.2],
        ['snake', 0.45, 348, -242, 0.07, 0.78, 13.0],
        ['slug', 0.41, 156, 17, 0.061, 0.50, 8.1],
        ['monkey', 0.38, 210, -102, 0.009, 0.66, 7.1],
    ],
    hiraishin: [
        ['mark', 0.29, 715, 328, 0.029, 0.12, 7.1],
        ['depart', 0.19, 542, -1784, 0.007, 0.59, 15.2],
        ['arrive', 0.31, 979, -622, 0.008, 0.20, 9.5],
    ],
    shadow_clone: [
        ['seal', 0.26, 345, 267, 0.052, 0.35, 7.5],
        ['form', 0.31, 163, -230, 0.008, 0.89, 10.2],
        ['dispel', 0.26, 214, -476, 0.009, 0.77, 13.0],
    ],
    body_rite: [
        ['gather', 0.44, 266, 105, 0.17, 0.21, 3.4],
        ['balance', 0.49, 510, 140, 0.04, 0.15, 5.8],
        ['stone', 0.48, 318, -304, 0.007, 0.95, 8.9],
        ['release', 0.30, 160, -135, 0.029, 0.69, 7.6],
        ['heart', 0.37, 69, -29, 0.009, 0.28, 10.8],
        ['gate', 0.39, 116, -64, 0.013, 0.92, 6.9],
        ['end', 0.66, 78, -66, 0.014, 0.68, 5.7],
    ],
    kurama_cloak: [
        ['rise', 0.61, 101, 89, 0.135, 0.69, 3.1],
        ['paw', 0.38, 99, -127, 0.011, 0.78, 9.1],
        ['sweep', 0.59, 148, -133, 0.064, 0.95, 7.9],
    ],
    final_chakra: [
        ['sword', 0.42, 159, -155, 0.045, 0.91, 7.6],
        ['armor', 0.23, 328, -394, 0.008, 0.68, 13.4],
        ['shatter', 0.60, 199, -172, 0.014, 0.93, 9.2],
        ['magatama', 0.36, 448, 712, 0.11, 0.22, 4.11],
        ['seal', 0.29, 331, -521, 0.014, 0.70, 9.6],
        ['kindling', 0.59, 512, 285, 0.098, 0.21, 4.4],
        ['dim', 0.38, 446, -360, 0.052, 0.13, 6.8],
        ['biju_build', 1.76, 67, 50, 0.43, 0.31, 0.44],
        ['biju_launch', 0.49, 84, -67, 0.012, 0.93, 5.6],
        ['biju_abort', 0.34, 134, -315, 0.037, 0.54, 8.4],
        ['biju_collapse', 1.08, 65, -43, 0.008, 0.85, 3.7],
    ],
    mokuton: [
        ['warn', 0.36, 108, -34, 0.09, 0.45, 2.8],
        ['grow', 0.44, 176, -123, 0.008, 0.80, 6.7],
        ['burial', 0.49, 119, -139, 0.008, 0.93, 7.0],
        ['grab', 0.27, 233, -74, 0.013, 0.73, 11.7],
        ['golem', 0.57, 105, -104, 0.018, 0.82, 3.3],
    ],
};

const family = process.argv[2];
if (!Object.hasOwn(profiles, family)) {
    throw new Error(`Choose a sound family: ${Object.keys(profiles).join(', ')}`);
}

for (const [name, duration, root, sweep, attack, texture, decay] of profiles[family]) {
    const pcm = Buffer.alloc(Math.round(rate * duration) * 2);
    let seed = 39371;
    let body = 0;
    let grain = 0;
    for (let i = 0; i < pcm.length / 2; i++) {
        const t = i / rate;
        seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
        const white = seed / 2147483648 - 1;
        body += (white - body) * 0.037;
        grain += (white - grain) * 0.19;
        const phase = 2 * Math.PI * t * (root + sweep * t * 0.5);
        const vibration = 0.83 + 0.17 * Math.sin(2 * Math.PI * t * (11 + root / 18));
        const chord = Math.sin(phase) * 0.22 + Math.sin(phase * 1.503) * 0.09 + Math.sin(phase * 2.035) * 0.035;
        const envelope = Math.min(1, t / attack, (duration - t) / 0.082) * Math.exp(-t * decay);
        const sample = (chord + texture * (body * 1.70 + (grain - body) * 0.96)) * vibration * envelope;
        pcm.writeInt16LE(Math.round(Math.tanh(sample) * 30700), i * 2);
    }
    const output = fileURLToPath(new URL(`../src/main/resources/assets/narutomod/sounds/jutsu/${family}_${name}.ogg`, import.meta.url));
    const result = spawnSync('ffmpeg', [
        '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', String(rate), '-ac', '1',
        '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', output,
    ], { input: pcm, stdio: ['pipe', 'inherit', 'inherit'] });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(`FFmpeg could not encode ${family} ${name}: ${result.status}`);
    }
}
