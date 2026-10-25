import { createReadStream, existsSync, statSync, watch } from 'node:fs';
import { readFile, stat } from 'node:fs/promises';
import { createServer } from 'node:http';
import { extname, resolve, sep } from 'node:path';
import { timingSafeEqual } from 'node:crypto';

const port = positivePort(process.env.PORT, 8789);
const catalogKey = process.env.DERBIS_CANVAS_CATALOG_KEY?.trim() ?? '';
const catalogFile = resolve(process.env.CATALOG_FILE ?? './catalog/catalog.json');
const mediaDir = resolve(process.env.MEDIA_DIR ?? './media');
const publicBaseUrl = cleanPublicBaseUrl(process.env.PUBLIC_BASE_URL);
const maxBodyBytes = 16 * 1024;
const maxEntries = 100_000;
const maxFieldLength = 512;
const durationToleranceMs = 2_000;

if (!catalogKey) {
  console.error('DERBIS_CANVAS_CATALOG_KEY is required. Refusing to start an unauthenticated catalog.');
  process.exit(1);
}

let catalog = new Map();
let catalogEntryCount = 0;
let reloadTimer;

await reloadCatalog();
watch(catalogFile, { persistent: false }, () => {
  clearTimeout(reloadTimer);
  reloadTimer = setTimeout(() => reloadCatalog().catch((error) => console.error('Catalog reload failed:', error.message)), 250);
});

const server = createServer(async (request, response) => {
  try {
    const url = new URL(request.url ?? '/', requestBaseUrl(request));
    if (url.pathname === '/healthz' && request.method === 'GET') {
      return json(response, 200, { status: 'ok', entries: catalogEntryCount });
    }
    if (url.pathname === '/v1/motion-artwork/resolve') {
      return await resolveArtwork(request, response);
    }
    if (url.pathname.startsWith('/media/')) {
      return await serveMedia(request, response, url.pathname);
    }
    return json(response, 404, { status: 'not_found' });
  } catch (error) {
    console.error('Unhandled catalog request error:', error);
    return json(response, 500, { status: 'error' });
  }
});

server.requestTimeout = 10_000;
server.headersTimeout = 12_000;
server.listen(port, '0.0.0.0', () => {
  console.log(`Derbis Canvas Catalog listening on http://0.0.0.0:${port}`);
  console.log(`Catalog: ${catalogFile}; media: ${mediaDir}; entries: ${catalogEntryCount}`);
});

async function resolveArtwork(request, response) {
  if (request.method !== 'POST') return json(response, 405, { status: 'method_not_allowed' }, { Allow: 'POST' });
  if (!authorized(request.headers['x-derbis-canvas-key'])) return json(response, 401, { status: 'unauthorized' });
  const contentType = String(request.headers['content-type'] ?? '').split(';', 1)[0].trim().toLowerCase();
  if (contentType !== 'application/json') return json(response, 415, { status: 'unsupported_media_type' });

  let query;
  try {
    query = validateLookup(JSON.parse(await readBody(request)));
  } catch (error) {
    return json(response, 400, { status: 'invalid_request' });
  }

  const entry = lookup(query);
  if (!entry) return json(response, 200, { status: 'miss' }, { 'Cache-Control': 'no-store' });

  const publicUrl = publicMediaUrl(request, entry.file);
  if (!publicUrl) {
    console.error('PUBLIC_BASE_URL must be configured for catalog resolution.');
    return json(response, 503, { status: 'not_configured' });
  }
  return json(response, 200, {
    status: 'resolved',
    url: publicUrl.toString(),
    mimeType: entry.mimeType,
    width: entry.width,
    height: entry.height,
    expiresAtMs: Date.now() + 7 * 24 * 60 * 60 * 1000
  }, { 'Cache-Control': 'private, max-age=300' });
}

async function serveMedia(request, response, pathname) {
  if (request.method !== 'GET' && request.method !== 'HEAD') {
    return json(response, 405, { status: 'method_not_allowed' }, { Allow: 'GET, HEAD' });
  }
  const relative = decodeURIComponent(pathname.slice('/media/'.length));
  if (!isSafeRelativePath(relative)) return json(response, 400, { status: 'invalid_media_path' });
  const mediaPath = resolve(mediaDir, relative);
  if (!mediaPath.startsWith(`${mediaDir}${sep}`)) return json(response, 400, { status: 'invalid_media_path' });

  let file;
  try {
    file = await stat(mediaPath);
  } catch {
    return json(response, 404, { status: 'not_found' });
  }
  if (!file.isFile()) return json(response, 404, { status: 'not_found' });

  const range = parseByteRange(request.headers.range, file.size);
  if (range === 'invalid') {
    response.writeHead(416, { 'Content-Range': `bytes */${file.size}`, 'X-Content-Type-Options': 'nosniff' });
    return response.end();
  }
  const partial = range !== undefined;
  const start = partial ? range.start : 0;
  const end = partial ? range.end : file.size - 1;
  const headers = {
    'Content-Type': mediaContentType(mediaPath),
    'Content-Length': String(end - start + 1),
    'Accept-Ranges': 'bytes',
    // Catalog media is immutable per filename. Use a new filename when replacing an asset.
    'Cache-Control': 'public, max-age=604800, immutable',
    'X-Content-Type-Options': 'nosniff',
    ...(partial ? { 'Content-Range': `bytes ${start}-${end}/${file.size}` } : {})
  };
  if (request.method === 'HEAD') {
    response.writeHead(partial ? 206 : 200, headers);
    return response.end();
  }
  response.writeHead(partial ? 206 : 200, headers);
  createReadStream(mediaPath, { start, end }).pipe(response);
}

async function reloadCatalog() {
  const raw = JSON.parse(await readFile(catalogFile, 'utf8'));
  if (!raw || raw.version !== 1 || !Array.isArray(raw.entries) || raw.entries.length > maxEntries) {
    throw new Error('catalog.json must use version 1 with a bounded entries array');
  }
  const next = new Map();
  for (const source of raw.entries) {
    const entry = validateCatalogEntry(source);
    const isrcKey = normalizeIsrc(entry.isrc);
    if (isrcKey) {
      if (next.has(`isrc:${isrcKey}`)) throw new Error(`duplicate ISRC: ${entry.isrc}`);
      next.set(`isrc:${isrcKey}`, entry);
    }
    const metadataKey = metadataLookupKey(entry);
    if (metadataKey) {
      if (next.has(`meta:${metadataKey}`)) throw new Error(`duplicate normalized metadata: ${entry.title} — ${entry.artist}`);
      next.set(`meta:${metadataKey}`, entry);
    }
  }
  catalog = next;
  catalogEntryCount = raw.entries.length;
  console.log(`Loaded ${catalogEntryCount} Derbis Canvas entries.`);
}

function lookup(query) {
  const isrc = normalizeIsrc(query.isrc);
  if (isrc) {
    const byIsrc = catalog.get(`isrc:${isrc}`);
    if (byIsrc) return byIsrc;
  }
  const direct = catalog.get(`meta:${metadataLookupKey(query)}`);
  if (direct && durationsAgree(direct.durationMs, query.durationMs)) return direct;
  // The duration varies slightly among distributors. Keep title/artist/album exact after
  // normalization and select only an unambiguous bounded-duration catalog record.
  for (const [key, entry] of catalog) {
    if (key.startsWith('meta:') && normalizedText(entry.title) === normalizedText(query.title) &&
      normalizedText(entry.artist) === normalizedText(query.artist) &&
      normalizedText(entry.album) === normalizedText(query.album) &&
      durationsAgree(entry.durationMs, query.durationMs)) return entry;
  }
  return undefined;
}

function validateLookup(source) {
  if (!source || typeof source !== 'object' || Array.isArray(source)) throw new Error('lookup must be an object');
  const query = {
    isrc: boundedText(source.isrc, 32),
    title: boundedText(source.title, maxFieldLength),
    artist: boundedText(source.artist, maxFieldLength),
    album: boundedText(source.album, maxFieldLength),
    durationMs: boundedDuration(source.durationMs)
  };
  if (!normalizeIsrc(query.isrc) && (!query.title || !query.artist || !query.album)) throw new Error('an ISRC or complete metadata is required');
  return query;
}

function validateCatalogEntry(source) {
  if (!source || typeof source !== 'object' || Array.isArray(source)) throw new Error('invalid catalog entry');
  const entry = {
    isrc: boundedText(source.isrc, 32),
    title: boundedText(source.title, maxFieldLength),
    artist: boundedText(source.artist, maxFieldLength),
    album: boundedText(source.album, maxFieldLength),
    durationMs: boundedDuration(source.durationMs),
    file: String(source.file ?? '').trim(),
    mimeType: String(source.mimeType ?? '').trim().toLowerCase(),
    width: positiveInteger(source.width, 0, 8192),
    height: positiveInteger(source.height, 0, 8192)
  };
  if (!normalizeIsrc(entry.isrc) && (!entry.title || !entry.artist || !entry.album)) throw new Error('entry needs an ISRC or complete metadata');
  if (!isSafeRelativePath(entry.file) || !/\.(mp4|m3u8)$/i.test(entry.file)) throw new Error(`invalid media file: ${entry.file}`);
  if (!['video/mp4', 'application/x-mpegurl'].includes(entry.mimeType)) throw new Error(`invalid mimeType: ${entry.mimeType}`);
  if ((entry.width === 0) !== (entry.height === 0)) throw new Error('width and height must be both set or both omitted');
  const localFile = resolve(mediaDir, entry.file);
  if (!localFile.startsWith(`${mediaDir}${sep}`) || !existsSync(localFile) || !statSync(localFile).isFile()) {
    throw new Error(`catalog media file does not exist: ${entry.file}`);
  }
  return entry;
}

function metadataLookupKey(value) {
  const title = normalizedText(value.title);
  const artist = normalizedText(value.artist);
  const album = normalizedText(value.album);
  return title && artist && album ? `${title}|${artist}|${album}` : '';
}

function normalizedText(value) {
  return String(value ?? '').normalize('NFKD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^\p{L}\p{N}]+/gu, ' ').trim();
}

function normalizeIsrc(value) {
  const clean = String(value ?? '').toUpperCase().replace(/[^A-Z0-9]/g, '');
  return /^[A-Z]{2}[A-Z0-9]{3}\d{7}$/.test(clean) ? clean : '';
}

function durationsAgree(catalogDuration, requestedDuration) {
  return !catalogDuration || !requestedDuration || Math.abs(catalogDuration - requestedDuration) <= durationToleranceMs;
}

function boundedText(value, maximum) {
  if (value === undefined || value === null) return '';
  if (typeof value !== 'string' || value.length > maximum) throw new Error('invalid text field');
  return value.trim();
}

function boundedDuration(value) {
  if (value === undefined || value === null || value === '') return 0;
  if (!Number.isSafeInteger(value) || value < 0 || value > 24 * 60 * 60 * 1000) throw new Error('invalid duration');
  return value;
}

function positiveInteger(value, fallback, maximum) {
  if (value === undefined || value === null || value === '') return fallback;
  if (!Number.isSafeInteger(value) || value < 1 || value > maximum) throw new Error('invalid dimensions');
  return value;
}

function isSafeRelativePath(path) {
  return Boolean(path) && !path.includes('\\') && !path.startsWith('/') && !path.split('/').some((part) => !part || part === '.' || part === '..');
}

function parseByteRange(value, size) {
  if (!value) return undefined;
  const match = /^bytes=(\d*)-(\d*)$/i.exec(String(value).trim());
  if (!match || size <= 0) return 'invalid';
  const [, rawStart, rawEnd] = match;
  if (!rawStart && !rawEnd) return 'invalid';
  let start;
  let end;
  if (!rawStart) {
    const suffixLength = Number(rawEnd);
    if (!Number.isSafeInteger(suffixLength) || suffixLength <= 0) return 'invalid';
    start = Math.max(0, size - suffixLength);
    end = size - 1;
  } else {
    start = Number(rawStart);
    end = rawEnd ? Number(rawEnd) : size - 1;
    if (!Number.isSafeInteger(start) || !Number.isSafeInteger(end) || start < 0 || end < start) return 'invalid';
    if (start >= size) return 'invalid';
    end = Math.min(end, size - 1);
  }
  return { start, end };
}

function mediaContentType(path) {
  switch (extname(path).toLowerCase()) {
    case '.mp4': return 'video/mp4';
    case '.m3u8': return 'application/x-mpegURL';
    case '.ts': return 'video/mp2t';
    case '.m4s': return 'video/iso.segment';
    default: return 'application/octet-stream';
  }
}

function publicMediaUrl(request, file) {
  const base = publicBaseUrl ?? new URL(requestBaseUrl(request));
  if (base.protocol !== 'https:' || base.port || base.username || base.password) return undefined;
  return new URL(`/media/${encodePath(file)}`, base);
}

function cleanPublicBaseUrl(value) {
  if (!value) return undefined;
  const url = new URL(value);
  if (url.protocol !== 'https:' || url.port || url.username || url.password || url.pathname !== '/' || url.search || url.hash) {
    throw new Error('PUBLIC_BASE_URL must be a bare HTTPS origin such as https://canvas.example.com');
  }
  return url;
}

function requestBaseUrl(request) {
  // This fallback is only for a TLS-terminating reverse proxy that passes Host unchanged.
  const host = String(request.headers.host ?? '').replace(/[\r\n]/g, '');
  return `https://${host || 'invalid.local'}`;
}

function encodePath(file) {
  return file.split('/').map(encodeURIComponent).join('/');
}

function authorized(value) {
  if (typeof value !== 'string') return false;
  const candidate = Buffer.from(value);
  const expected = Buffer.from(catalogKey);
  return candidate.length === expected.length && timingSafeEqual(candidate, expected);
}

function readBody(request) {
  return new Promise((resolveBody, reject) => {
    let total = 0;
    const chunks = [];
    request.on('data', (chunk) => {
      total += chunk.length;
      if (total > maxBodyBytes) {
        reject(new Error('request body too large'));
        request.destroy();
        return;
      }
      chunks.push(chunk);
    });
    request.on('end', () => resolveBody(Buffer.concat(chunks).toString('utf8')));
    request.on('error', reject);
  });
}

function json(response, status, body, headers = {}) {
  response.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': String(Buffer.byteLength(JSON.stringify(body))),
    'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff',
    ...headers
  });
  response.end(JSON.stringify(body));
}

function positivePort(value, fallback) {
  const parsed = Number(value ?? fallback);
  return Number.isSafeInteger(parsed) && parsed > 0 && parsed <= 65535 ? parsed : fallback;
}
