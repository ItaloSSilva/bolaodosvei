const http = require("http");
const fs = require("fs");
const path = require("path");

const PORT = process.env.PORT || 3000;
const DATA_FILE = path.join(__dirname, "salas_db.json");
const MAX_BODY_BYTES = 256 * 1024;
const MAX_CHAT_MESSAGES = 100;
const MAX_CHAT_TEXT_LENGTH = 500;
const RESERVED_KEYS = new Set(["__proto__", "constructor", "prototype"]);

function isReservedKey(key) {
  return RESERVED_KEYS.has(key) || RESERVED_KEYS.has(key.toLowerCase());
}

function loadDb() {
  try {
    if (fs.existsSync(DATA_FILE)) {
      const parsed = JSON.parse(fs.readFileSync(DATA_FILE, "utf8"));
      if (parsed && typeof parsed === "object" && parsed.salas && typeof parsed.salas === "object") {
        return parsed;
      }
    }
  } catch (e) {
    console.error("Erro ao ler banco:", e.message);
  }
  return { salas: {} };
}

let saveQueue = Promise.resolve();

function saveDb(dbSnapshot) {
  const tmpFile = `${DATA_FILE}.tmp`;
  const serialized = JSON.stringify(dbSnapshot, null, 2);
  const run = saveQueue.then(async () => {
    await fs.promises.writeFile(tmpFile, serialized, "utf8");
    await fs.promises.rename(tmpFile, DATA_FILE);
  });
  saveQueue = run.catch(() => {});
  return run;
}

function readJsonBody(req, res) {
  return new Promise((resolve) => {
    const chunks = [];
    let totalBytes = 0;
    let settled = false;

    req.on("data", (chunk) => {
      if (settled) return;
      totalBytes += chunk.length;
      if (totalBytes > MAX_BODY_BYTES) {
        settled = true;
        res.writeHead(413);
        res.end(JSON.stringify({ error: "Payload muito grande" }));
        req.destroy();
        resolve(null);
        return;
      }
      chunks.push(chunk);
    });

    req.on("error", (err) => {
      if (settled) return;
      settled = true;
      console.error("Erro na requisição:", err.message);
      if (!res.headersSent) {
        res.writeHead(400);
        res.end(JSON.stringify({ error: "Erro ao ler requisição" }));
      } else if (!res.writableEnded) {
        res.end();
      }
      resolve(null);
    });

    req.on("end", () => {
      if (settled) return;
      settled = true;
      let payload;
      try {
        const body = Buffer.concat(chunks).toString("utf8");
        payload = JSON.parse(body);
      } catch (err) {
        res.writeHead(400);
        res.end(JSON.stringify({ error: "JSON inválido" }));
        resolve(null);
        return;
      }

      if (payload === null || typeof payload !== "object" || Array.isArray(payload)) {
        res.writeHead(400);
        res.end(JSON.stringify({ error: "Payload deve ser um objeto JSON" }));
        resolve(null);
        return;
      }

      resolve(payload);
    });
  });
}

const db = loadDb();

const server = http.createServer(async (req, res) => {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET, PUT, POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");
  res.setHeader("Content-Type", "application/json; charset=utf-8");

  if (req.method === "OPTIONS") {
    res.writeHead(204);
    res.end();
    return;
  }

  const urlPath = req.url.split("?")[0];

  // Healthcheck
  if (req.method === "GET" && urlPath === "/") {
    res.writeHead(200);
    res.end(JSON.stringify({ status: "ok", app: "Bolao dos Vei Sync Server" }));
    return;
  }

  // GET /salas/:codigo/participantes(.json)
  const getMatch = urlPath.match(/^\/salas\/([A-Za-z0-9_-]+)\/participantes(?:\.json)?$/);
  if (req.method === "GET" && getMatch) {
    const rawRoom = getMatch[1];
    const room = rawRoom.toUpperCase();
    if (isReservedKey(rawRoom) || isReservedKey(room)) {
      res.writeHead(400);
      res.end(JSON.stringify({ error: "Identificador inválido" }));
      return;
    }
    const roomEntry = Object.prototype.hasOwnProperty.call(db.salas, room) ? db.salas[room] : null;
    const participants = (roomEntry && roomEntry.participantes) || {};
    res.writeHead(200);
    res.end(JSON.stringify(participants));
    return;
  }

  // PUT /salas/:codigo/participantes/:userId(.json)
  const putMatch = urlPath.match(/^\/salas\/([A-Za-z0-9_-]+)\/participantes\/([A-Za-z0-9_-]+)(?:\.json)?$/);
  if (req.method === "PUT" && putMatch) {
    const rawRoom = putMatch[1];
    const room = rawRoom.toUpperCase();
    const userId = putMatch[2];
    if (isReservedKey(rawRoom) || isReservedKey(room) || isReservedKey(userId)) {
      res.writeHead(400);
      res.end(JSON.stringify({ error: "Identificador inválido" }));
      return;
    }

    const payload = await readJsonBody(req, res);
    if (!payload) return;

    if (!Object.prototype.hasOwnProperty.call(db.salas, room)) {
      db.salas[room] = { participantes: {}, chat: [] };
    }
    if (!db.salas[room].participantes || typeof db.salas[room].participantes !== "object") {
      db.salas[room].participantes = {};
    }
    db.salas[room].participantes[userId] = payload;

    try {
      await saveDb(db);
    } catch (saveErr) {
      console.error("Erro ao salvar banco:", saveErr.message);
      res.writeHead(500);
      res.end(JSON.stringify({ error: "Erro ao persistir dados" }));
      return;
    }

    res.writeHead(200);
    res.end(JSON.stringify({ ok: true, room, userId }));
    return;
  }

  // GET /salas/:codigo/chat(.json)
  const getChatMatch = urlPath.match(/^\/salas\/([A-Za-z0-9_-]+)\/chat(?:\.json)?$/);
  if (req.method === "GET" && getChatMatch) {
    const rawRoom = getChatMatch[1];
    const room = rawRoom.toUpperCase();
    if (isReservedKey(rawRoom) || isReservedKey(room)) {
      res.writeHead(400);
      res.end(JSON.stringify({ error: "Identificador inválido" }));
      return;
    }
    const roomEntry = Object.prototype.hasOwnProperty.call(db.salas, room) ? db.salas[room] : null;
    const chat = (roomEntry && Array.isArray(roomEntry.chat)) ? roomEntry.chat : [];
    res.writeHead(200);
    res.end(JSON.stringify(chat));
    return;
  }

  // POST /salas/:codigo/chat(.json)
  const postChatMatch = urlPath.match(/^\/salas\/([A-Za-z0-9_-]+)\/chat(?:\.json)?$/);
  if (req.method === "POST" && postChatMatch) {
    const rawRoom = postChatMatch[1];
    const room = rawRoom.toUpperCase();
    if (isReservedKey(rawRoom) || isReservedKey(room)) {
      res.writeHead(400);
      res.end(JSON.stringify({ error: "Identificador inválido" }));
      return;
    }

    const payload = await readJsonBody(req, res);
    if (!payload) return;

    const trimmedText = typeof payload.text === "string" ? payload.text.trim() : "";
    const trimmedSenderName = typeof payload.senderName === "string" ? payload.senderName.trim() : "";
    if (!trimmedText || trimmedText.length > MAX_CHAT_TEXT_LENGTH || !trimmedSenderName) {
      res.writeHead(400);
      res.end(JSON.stringify({ error: "Mensagem ou remetente inválido" }));
      return;
    }

    const message = {
      id: typeof payload.id === "string" && payload.id.trim()
        ? payload.id.trim().slice(0, 128)
        : `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      senderId: typeof payload.senderId === "string" ? payload.senderId.trim().slice(0, 128) : "",
      senderName: trimmedSenderName.slice(0, 80),
      senderAvatarColorHex: typeof payload.senderAvatarColorHex === "number" && Number.isFinite(payload.senderAvatarColorHex)
        ? payload.senderAvatarColorHex
        : 0xff00c853,
      senderTitle: typeof payload.senderTitle === "string" ? payload.senderTitle.trim().slice(0, 80) : "",
      senderFlair: typeof payload.senderFlair === "string" ? payload.senderFlair.trim().slice(0, 80) : "",
      text: trimmedText,
      timestampMillis: typeof payload.timestampMillis === "number" && Number.isFinite(payload.timestampMillis)
        ? payload.timestampMillis
        : Date.now(),
    };

    if (!Object.prototype.hasOwnProperty.call(db.salas, room)) {
      db.salas[room] = { participantes: {}, chat: [] };
    }
    if (!Array.isArray(db.salas[room].chat)) {
      db.salas[room].chat = [];
    }
    db.salas[room].chat.push(message);
    if (db.salas[room].chat.length > MAX_CHAT_MESSAGES) {
      db.salas[room].chat = db.salas[room].chat.slice(-MAX_CHAT_MESSAGES);
    }

    try {
      await saveDb(db);
    } catch (saveErr) {
      console.error("Erro ao salvar banco:", saveErr.message);
      res.writeHead(500);
      res.end(JSON.stringify({ error: "Erro ao persistir dados" }));
      return;
    }

    res.writeHead(200);
    res.end(JSON.stringify({ ok: true, count: db.salas[room].chat.length }));
    return;
  }

  res.writeHead(404);
  res.end(JSON.stringify({ error: "Rota não encontrada" }));
});

server.listen(PORT, () => {
  console.log(`Servidor Bolão dos Véi rodando na porta ${PORT}`);
});
