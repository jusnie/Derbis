import http from 'node:http';
import crypto from 'node:crypto';
import { WebSocketServer, WebSocket } from 'ws';

const port = Number.parseInt(process.env.PORT ?? '8787', 10);
const maxPayload = 16 * 1024;
const maxRoomClients = 8;
const roomIdleMs = 5 * 60 * 1000;
const heartbeatMs = 30 * 1000;
const allowedOrigins = new Set(
  (process.env.JAM_ALLOWED_ORIGINS ?? '')
    .split(',')
    .map((value) => value.trim())
    .filter(Boolean)
);

/** @type {Map<string, {host?: Client, guests: Map<string, Client>, idleTimer?: NodeJS.Timeout}>} */
const rooms = new Map();

/** @typedef {{ id: string, socket: import('ws').WebSocket, room?: string, role?: 'host'|'guest', alive: boolean }} Client */

const server = http.createServer((request, response) => {
  if (request.url === '/health') {
    response.writeHead(200, { 'content-type': 'application/json', 'cache-control': 'no-store' });
    response.end(JSON.stringify({ ok: true, rooms: rooms.size }));
    return;
  }
  response.writeHead(404, { 'content-type': 'application/json' });
  response.end(JSON.stringify({ error: 'not_found' }));
});

const wss = new WebSocketServer({
  noServer: true,
  maxPayload,
  perMessageDeflate: false,
  clientTracking: true
});

server.on('upgrade', (request, socket, head) => {
  const origin = request.headers.origin;
  if (allowedOrigins.size > 0 && origin && !allowedOrigins.has(origin)) {
    socket.write('HTTP/1.1 403 Forbidden\r\nConnection: close\r\n\r\n');
    socket.destroy();
    return;
  }
  if (request.url?.split('?')[0] !== '/jam') {
    socket.write('HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\n');
    socket.destroy();
    return;
  }
  wss.handleUpgrade(request, socket, head, (websocket) => wss.emit('connection', websocket));
});

wss.on('connection', (socket) => {
  /** @type {Client} */
  const client = { id: crypto.randomUUID(), socket, alive: true };
  socket.isAlive = true;
  socket.on('pong', () => { socket.isAlive = true; });
  socket.on('error', () => {});
  socket.on('message', (raw, isBinary) => {
    if (isBinary || raw.length > maxPayload) {
      closeWith(client, 1009, 'message_too_large');
      return;
    }
    let message;
    try {
      message = JSON.parse(raw.toString('utf8'));
    } catch {
      closeWith(client, 1003, 'invalid_json');
      return;
    }
    handleMessage(client, message);
  });
  socket.on('close', () => leaveRoom(client));
});

function handleMessage(client, message) {
  if (!message || typeof message !== 'object' || Array.isArray(message)) {
    closeWith(client, 1003, 'invalid_message');
    return;
  }
  if (message.type === 'hello') {
    joinRoom(client, message);
    return;
  }
  if (message.type === 'ping') {
    send(client, { type: 'pong' });
    return;
  }
  if (message.type === 'message') {
    routeMessage(client, message);
    return;
  }
  closeWith(client, 1003, 'unknown_message');
}

function joinRoom(client, message) {
  if (client.room) {
    closeWith(client, 1008, 'already_joined');
    return;
  }
  const roomKey = typeof message.room === 'string' ? message.room.toLowerCase() : '';
  const role = message.role;
  if (!/^[a-f0-9]{32}$/.test(roomKey) || (role !== 'host' && role !== 'guest')) {
    closeWith(client, 1008, 'invalid_room');
    return;
  }

  const room = rooms.get(roomKey) ?? { guests: new Map() };
  if (room.idleTimer) clearTimeout(room.idleTimer);
  room.idleTimer = undefined;

  if (role === 'host') {
    if (room.host) {
      send(client, { type: 'failure', reason: 'host_exists' });
      closeWith(client, 1008, 'host_exists');
      return;
    }
    room.host = client;
    client.room = roomKey;
    client.role = 'host';
    rooms.set(roomKey, room);
    send(client, { type: 'ready', clientId: client.id });
    return;
  }

  if (!room.host) {
    send(client, { type: 'failure', reason: 'host_unavailable' });
    closeWith(client, 1008, 'host_unavailable');
    return;
  }
  if (room.guests.size >= maxRoomClients - 1) {
    send(client, { type: 'failure', reason: 'room_full' });
    closeWith(client, 1008, 'room_full');
    return;
  }
  room.guests.set(client.id, client);
  client.room = roomKey;
  client.role = 'guest';
  rooms.set(roomKey, room);
  send(client, { type: 'ready', clientId: client.id });
  send(room.host, { type: 'peer_joined', clientId: client.id });
}

function routeMessage(client, message) {
  if (!client.room || !client.role || typeof message.payload !== 'string' || message.payload.length > maxPayload) {
    closeWith(client, 1008, 'invalid_message');
    return;
  }
  const room = rooms.get(client.room);
  if (!room) {
    closeWith(client, 1008, 'room_not_found');
    return;
  }
  if (client.role === 'guest') {
    if (!room.host) {
      send(client, { type: 'failure', reason: 'host_unavailable' });
      closeWith(client, 1008, 'host_unavailable');
      return;
    }
    send(room.host, { type: 'message', from: client.id, payload: message.payload });
    return;
  }
  const target = typeof message.to === 'string' ? room.guests.get(message.to) : undefined;
  if (target) send(target, { type: 'message', from: 'host', payload: message.payload });
}

function leaveRoom(client) {
  if (!client.room || !client.role) return;
  const room = rooms.get(client.room);
  if (!room) return;
  if (client.role === 'host' && room.host === client) {
    room.host = undefined;
    for (const guest of room.guests.values()) {
      send(guest, { type: 'failure', reason: 'host_unavailable' });
      closeWith(guest, 1001, 'host_disconnected');
    }
    room.guests.clear();
  } else if (client.role === 'guest') {
    room.guests.delete(client.id);
    if (room.host) send(room.host, { type: 'peer_left', clientId: client.id });
  }
  if (!room.host && room.guests.size === 0) {
    room.idleTimer = setTimeout(() => {
      const stale = rooms.get(client.room);
      if (stale && !stale.host && stale.guests.size === 0) rooms.delete(client.room);
    }, roomIdleMs);
  }
  client.room = undefined;
  client.role = undefined;
}

function send(client, value) {
  if (client.socket.readyState === WebSocket.OPEN) {
    client.socket.send(JSON.stringify(value));
  }
}

function closeWith(client, code, reason) {
  if (client.socket.readyState === WebSocket.OPEN || client.socket.readyState === WebSocket.CONNECTING) {
    client.socket.close(code, reason);
  }
}

const heartbeat = setInterval(() => {
  for (const socket of wss.clients) {
    if (socket.isAlive === false) {
      socket.terminate();
      continue;
    }
    socket.isAlive = false;
    socket.ping();
  }
}, heartbeatMs);

server.listen(port, '0.0.0.0', () => {
  console.log(`Derbis Jam relay listening on :${port}`);
});

function shutdown() {
  clearInterval(heartbeat);
  for (const socket of wss.clients) socket.close(1001, 'server_shutdown');
  server.close(() => process.exit(0));
  setTimeout(() => process.exit(0), 5000).unref();
}
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
