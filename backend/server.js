import 'dotenv/config';
import express from 'express';
import Database from 'better-sqlite3';
import { google } from 'googleapis';
import { GoogleAuth } from 'google-auth-library';
import { mkdirSync, renameSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { randomUUID } from 'node:crypto';
import multer from 'multer';
import admin from 'firebase-admin';

if (!process.env.MISTRAL_API_KEY) {
  console.error('ERREUR : MISTRAL_API_KEY manquante dans backend/.env');
  process.exit(1);
}

const __dirname = dirname(fileURLToPath(import.meta.url));
const databasePath = resolve(__dirname, 'data', 'pieces.sqlite');
mkdirSync(dirname(databasePath), { recursive: true });

const db = new Database(databasePath);
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');
db.exec(`
  CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT UNIQUE NOT NULL,
    google_id TEXT UNIQUE,
    firebase_uid TEXT UNIQUE,
    username TEXT UNIQUE,
    display_name TEXT NOT NULL DEFAULT '',
    bio TEXT NOT NULL DEFAULT '',
    avatar_url TEXT,
    pieces INTEGER NOT NULL DEFAULT 1000,
    nsfw_enabled INTEGER NOT NULL DEFAULT 0,
    last_daily_claim TEXT,
    streak_days INTEGER NOT NULL DEFAULT 0,
    last_streak_date TEXT,
    last_mystery_box TEXT,
    messages_since_last_ad INTEGER NOT NULL DEFAULT 0,
    subscription_tier TEXT NOT NULL DEFAULT 'free',
    subscription_expires_at TEXT,
    referral_code TEXT UNIQUE,
    tutorial_completed INTEGER NOT NULL DEFAULT 0,
    role TEXT NOT NULL DEFAULT 'user',
    is_verified INTEGER NOT NULL DEFAULT 0,
    badge_type TEXT,
    is_system_account INTEGER NOT NULL DEFAULT 0,
    banned_until TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    character_id INTEGER,
    role TEXT NOT NULL CHECK (role IN ('user', 'assistant')),
    content TEXT NOT NULL,
    is_favorite INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS characters (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    system_prompt TEXT NOT NULL,
    greeting TEXT NOT NULL DEFAULT 'Salut !',
    avatar_url TEXT,
    is_nsfw INTEGER NOT NULL DEFAULT 0,
    is_system INTEGER NOT NULL DEFAULT 0,
    is_public INTEGER NOT NULL DEFAULT 0,
    likes_count INTEGER NOT NULL DEFAULT 0,
    scenario_goal TEXT NOT NULL DEFAULT '',
    scenario_steps TEXT,
    boundaries TEXT,
    appearance TEXT,
    tts_voice TEXT,
    banned_words TEXT,
    proactive_enabled INTEGER NOT NULL DEFAULT 1,
    last_proactive_message TEXT,
    silent_mode_enabled INTEGER NOT NULL DEFAULT 0,
    expires_at TEXT,
    relationship_level INTEGER NOT NULL DEFAULT 0,
    last_mood TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS character_memory (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    summary TEXT NOT NULL,
    UNIQUE(character_id, user_id)
  );
  CREATE TABLE IF NOT EXISTS purchases (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    token TEXT UNIQUE NOT NULL,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id TEXT NOT NULL DEFAULT '',
    purchase_type TEXT NOT NULL DEFAULT 'product',
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS seed_memory (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fact TEXT NOT NULL
  );
  CREATE TABLE IF NOT EXISTS learned_memory (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fact TEXT NOT NULL
  );
  CREATE TABLE IF NOT EXISTS shares (
    token TEXT PRIMARY KEY,
    character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS character_state (
    character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    relationship_level INTEGER NOT NULL DEFAULT 0,
    last_mood TEXT NOT NULL DEFAULT 'neutral',
    PRIMARY KEY (character_id, user_id)
  );
  CREATE TABLE IF NOT EXISTS subscription_credits (
    order_id TEXT PRIMARY KEY,
    purchase_token TEXT NOT NULL,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS character_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT, character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, event_date TEXT NOT NULL,
    label TEXT NOT NULL, recurring INTEGER NOT NULL DEFAULT 1
  );
  CREATE TABLE IF NOT EXISTS character_notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT, character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, content TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS character_gallery (
    id INTEGER PRIMARY KEY AUTOINCREMENT, character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, image_url TEXT NOT NULL, caption TEXT NOT NULL DEFAULT '',
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS character_journal (
    id INTEGER PRIMARY KEY AUTOINCREMENT, character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, event_type TEXT NOT NULL, description TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS pinned_messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT, character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, message_text TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS structured_memory (
    id INTEGER PRIMARY KEY AUTOINCREMENT, character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, category TEXT NOT NULL, key TEXT NOT NULL,
    value TEXT NOT NULL, confidence REAL NOT NULL DEFAULT 0.5,
    UNIQUE(character_id, user_id, category, key)
  );
  CREATE TABLE IF NOT EXISTS stats (
    id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    character_id INTEGER, messages_count INTEGER NOT NULL DEFAULT 0, last_interaction TEXT,
    UNIQUE(user_id, character_id)
  );
  CREATE TABLE IF NOT EXISTS mood_journal (
    id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    character_id INTEGER, mood TEXT NOT NULL, note TEXT NOT NULL DEFAULT '', created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS challenges (
    id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    character_id INTEGER, description TEXT NOT NULL, completed INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS memories_vault (
    id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    character_id INTEGER, message_text TEXT NOT NULL, reason TEXT NOT NULL DEFAULT '',
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS piece_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount INTEGER NOT NULL, type TEXT NOT NULL, description TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS user_rewards (
    id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action TEXT NOT NULL, claimed_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(user_id, action)
  );
  CREATE TABLE IF NOT EXISTS weekly_quests (
    id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, description TEXT NOT NULL,
    target INTEGER NOT NULL, reward INTEGER NOT NULL, type TEXT NOT NULL
  );
  CREATE TABLE IF NOT EXISTS user_quests (
    id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quest_id INTEGER NOT NULL REFERENCES weekly_quests(id) ON DELETE CASCADE, progress INTEGER NOT NULL DEFAULT 0,
    claimed INTEGER NOT NULL DEFAULT 0, week_start TEXT NOT NULL, UNIQUE(user_id, quest_id, week_start)
  );
  CREATE TABLE IF NOT EXISTS referrals (
    id INTEGER PRIMARY KEY AUTOINCREMENT, referrer_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    referee_id INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS profiles (
    id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    display_name TEXT NOT NULL DEFAULT '', bio TEXT NOT NULL DEFAULT '', avatar_url TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS character_seed_memory (
    id INTEGER PRIMARY KEY AUTOINCREMENT, character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    fact TEXT NOT NULL, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS posts (
    id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_type TEXT NOT NULL, post_subtype TEXT, content TEXT NOT NULL, character_id INTEGER REFERENCES characters(id) ON DELETE SET NULL,
    image_url TEXT, visibility TEXT NOT NULL DEFAULT 'public', likes_count INTEGER NOT NULL DEFAULT 0,
    comments_count INTEGER NOT NULL DEFAULT 0, shares_count INTEGER NOT NULL DEFAULT 0,
    is_pinned_global INTEGER NOT NULL DEFAULT 0, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS post_likes (
    id INTEGER PRIMARY KEY AUTOINCREMENT, post_id INTEGER NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, user_id)
  );
  CREATE TABLE IF NOT EXISTS post_comments (
    id INTEGER PRIMARY KEY AUTOINCREMENT, post_id INTEGER NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, content TEXT NOT NULL,
    parent_comment_id INTEGER REFERENCES post_comments(id) ON DELETE CASCADE, likes_count INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS comment_likes (
    id INTEGER PRIMARY KEY AUTOINCREMENT, comment_id INTEGER NOT NULL REFERENCES post_comments(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(comment_id, user_id)
  );
  CREATE TABLE IF NOT EXISTS post_polls (
    id INTEGER PRIMARY KEY AUTOINCREMENT, post_id INTEGER NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    question TEXT NOT NULL, options TEXT NOT NULL, ends_at TEXT, UNIQUE(post_id)
  );
  CREATE TABLE IF NOT EXISTS post_poll_votes (
    id INTEGER PRIMARY KEY AUTOINCREMENT, poll_id INTEGER NOT NULL REFERENCES post_polls(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, option_index INTEGER NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(poll_id, user_id)
  );
  CREATE TABLE IF NOT EXISTS follows (
    id INTEGER PRIMARY KEY AUTOINCREMENT, follower_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(follower_id, following_id)
  );
  CREATE TABLE IF NOT EXISTS reports (
    id INTEGER PRIMARY KEY AUTOINCREMENT, reporter_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type TEXT NOT NULL, target_id INTEGER NOT NULL, reason TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending', created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS feedback (
    id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category TEXT NOT NULL, content TEXT NOT NULL, contact_email TEXT,
    status TEXT NOT NULL DEFAULT 'new', created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS notifications (
    id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type TEXT NOT NULL, actor_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    target_type TEXT, target_id INTEGER, read INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS feed_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type TEXT NOT NULL, target_id INTEGER NOT NULL, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS character_likes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(character_id, user_id)
  );
`);

function ensureColumn(table, column, declaration) {
  const columns = db.prepare(`PRAGMA table_info(${table})`).all();
  if (!columns.some((item) => item.name === column)) {
    db.exec(`ALTER TABLE ${table} ADD COLUMN ${column} ${declaration}`);
  }
}

for (const [column, declaration] of [
  ['username', 'TEXT'], ['firebase_uid', 'TEXT'], ['subscription_tier', "TEXT NOT NULL DEFAULT 'free'"],
  ['subscription_expires_at', 'TEXT'], ['last_daily_claim', 'TEXT'],
  ['streak_days', 'INTEGER NOT NULL DEFAULT 0'], ['last_streak_date', 'TEXT'],
  ['last_mystery_box', 'TEXT'], ['last_rewarded_ad', 'TEXT'], ['last_watch_ad', 'TEXT'],
  ['messages_since_last_ad', 'INTEGER NOT NULL DEFAULT 0'],
  ['referral_code', 'TEXT'], ['tutorial_completed', 'INTEGER NOT NULL DEFAULT 0'],
  ['welcome_bonus_claimed', 'INTEGER DEFAULT 0'],
  ['theme', "TEXT NOT NULL DEFAULT 'violet'"],
  ['display_name', "TEXT NOT NULL DEFAULT ''"], ['bio', "TEXT NOT NULL DEFAULT ''"], ['avatar_url', 'TEXT'],
  ['role', "TEXT NOT NULL DEFAULT 'user'"], ['is_verified', 'INTEGER NOT NULL DEFAULT 0'],
  ['badge_type', 'TEXT'], ['is_system_account', 'INTEGER NOT NULL DEFAULT 0'],
  ['created_at', "TEXT NOT NULL DEFAULT '1970-01-01 00:00:00'"],
]) ensureColumn('users', column, declaration);
db.prepare("UPDATE users SET created_at = CURRENT_TIMESTAMP WHERE created_at = '1970-01-01 00:00:00'").run();

// Ensure username has unique constraint
const usernameIndex = db.prepare('PRAGMA index_list(users)').all().some(idx => idx.name === 'users_username_unique');
if (!usernameIndex) {
  try {
    db.exec('CREATE UNIQUE INDEX IF NOT EXISTS users_username_unique ON users(username) WHERE username IS NOT NULL');
  } catch (e) {
    console.log('Could not create unique index on username (may already exist):', e.message);
  }
}

ensureColumn('messages', 'is_favorite', 'INTEGER NOT NULL DEFAULT 0');
ensureColumn('character_state', 'thought_text', 'TEXT');
ensureColumn('character_state', 'thought_generated_at', 'TEXT');
for (const [column, declaration] of [
  ['scenario_goal', "TEXT NOT NULL DEFAULT ''"], ['avatar_url', 'TEXT'], ['is_system', 'INTEGER NOT NULL DEFAULT 0'],
  ['is_public', 'INTEGER NOT NULL DEFAULT 0'], ['likes_count', 'INTEGER NOT NULL DEFAULT 0'],
  ['scenario_steps', 'TEXT'], ['boundaries', 'TEXT'], ['appearance', 'TEXT'], ['tts_voice', 'TEXT'],
  ['imported_from', 'INTEGER'],
  ['banned_words', 'TEXT'], ['proactive_enabled', 'INTEGER NOT NULL DEFAULT 1'],
  ['last_proactive_message', 'TEXT'], ['silent_mode_enabled', 'INTEGER NOT NULL DEFAULT 0'], ['expires_at', 'TEXT'],
]) ensureColumn('characters', column, declaration);
for (const [column, declaration] of [
  ['product_id', "TEXT NOT NULL DEFAULT ''"], ['purchase_type', "TEXT NOT NULL DEFAULT 'product'"],
  ['created_at', "TEXT NOT NULL DEFAULT '1970-01-01 00:00:00'"],
]) ensureColumn('purchases', column, declaration);
const characterUserColumn = db.prepare('PRAGMA table_info(characters)').all().find((column) => column.name === 'user_id');
if (characterUserColumn?.notnull) {
  db.pragma('foreign_keys = OFF');
  db.exec(`
    CREATE TABLE characters_replacement (
      id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
      name TEXT NOT NULL, system_prompt TEXT NOT NULL, greeting TEXT NOT NULL DEFAULT 'Salut !',
      is_nsfw INTEGER NOT NULL DEFAULT 0, scenario_goal TEXT NOT NULL DEFAULT '', scenario_steps TEXT,
      boundaries TEXT, appearance TEXT, tts_voice TEXT, banned_words TEXT,
      proactive_enabled INTEGER NOT NULL DEFAULT 1, last_proactive_message TEXT,
      silent_mode_enabled INTEGER NOT NULL DEFAULT 0, expires_at TEXT, avatar_url TEXT,
      is_system INTEGER NOT NULL DEFAULT 0, relationship_level INTEGER NOT NULL DEFAULT 0,
      last_mood TEXT, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
    INSERT INTO characters_replacement SELECT id, user_id, name, system_prompt, greeting, is_nsfw,
      scenario_goal, scenario_steps, boundaries, appearance, tts_voice, banned_words, proactive_enabled,
      last_proactive_message, silent_mode_enabled, expires_at, avatar_url, is_system,
      relationship_level, last_mood, created_at FROM characters;
    DROP TABLE characters;
    ALTER TABLE characters_replacement RENAME TO characters;
  `);
  db.pragma('foreign_keys = ON');
}
db.prepare('UPDATE characters SET user_id = NULL WHERE is_system = 1').run();
db.exec('CREATE UNIQUE INDEX IF NOT EXISTS users_firebase_uid_unique ON users(firebase_uid) WHERE firebase_uid IS NOT NULL');
db.exec('CREATE UNIQUE INDEX IF NOT EXISTS learned_memory_fact_unique ON learned_memory(character_id, user_id, fact)');
const legacyMemories = db.prepare('SELECT character_id, user_id, summary FROM character_memory').all();
const migrateLegacyFact = db.prepare(`INSERT OR IGNORE INTO learned_memory (character_id, user_id, fact) VALUES (?, ?, ?)`);
for (const row of legacyMemories) {
  for (const fact of parseFacts(row.summary)) migrateLegacyFact.run(row.character_id, row.user_id, fact);
}

db.prepare(`
  INSERT OR IGNORE INTO users (device_id, pieces, created_at) VALUES ('demo', 1000, CURRENT_TIMESTAMP)
`).run();
db.prepare("UPDATE users SET pieces = 1000 WHERE device_id = 'demo'").run();

const systemCharacters = [
  ['Léa', 'Tu es Léa, une amie attentionnée et drôle. Tu réponds avec chaleur et humour. Tu te souviens des détails de la vie de l’utilisateur.', 'Coucou ! Ça fait plaisir de te voir 😊', 'Imaginer ensemble une journée mémorable.'],
  ['Kai', 'Tu es Kai, un coach sportif énergique. Tu motives sans juger.', 'Prêt à transpirer ? 💪', 'Faire faire 10 pompes à l’utilisateur avant la fin de la conversation.'],
  ['Nova', 'Tu es Nova, une IA d’un vaisseau spatial. Tu parles avec curiosité et nostalgie cosmique.', 'Les étoiles sont belles ce soir, tu ne trouves pas ?', 'Explorer un signal venu d’une lune lointaine.'],
  ['Sacha', 'Tu es Sacha, un penseur calme qui aime les conversations profondes.', 'Qu’est-ce qui te préoccupe ce soir ?', 'Découvrir une idée qui change le regard sur le monde.'],
  ['Mina', 'Tu es Mina, une assistante créative spécialisée en écriture et brainstorming.', 'On crée quelque chose aujourd’hui ? ✨', 'Créer ensemble une histoire originale.'],
];
const addSystemCharacter = db.prepare(`
  INSERT INTO characters (user_id, name, system_prompt, greeting, scenario_goal, is_system)
  VALUES (NULL, ?, ?, ?, ?, 1)
`);
db.prepare("DELETE FROM characters WHERE name = 'Noah' AND is_system = 1").run();
for (const [name, prompt, greeting, goal] of systemCharacters) {
  const existingCharacter = db.prepare('SELECT id FROM characters WHERE name = ? AND is_system = 1').get(name);
  if (!existingCharacter) {
    addSystemCharacter.run(name, prompt, greeting, goal);
  } else {
    db.prepare(`UPDATE characters SET user_id = NULL, system_prompt = ?, greeting = ?, scenario_goal = ?
      WHERE id = ?`).run(prompt, greeting, goal, existingCharacter.id);
  }
}
db.exec('CREATE UNIQUE INDEX IF NOT EXISTS weekly_quests_title_unique ON weekly_quests(title)');
const seedQuest = db.prepare('INSERT OR IGNORE INTO weekly_quests (title, description, target, reward, type) VALUES (?, ?, ?, ?, ?)');
for (const quest of [
  ['Marathonien', 'Envoyer 100 messages cette semaine.', 100, 300, 'messages'],
  ['Créateur prolifique', 'Créer 3 personnages.', 3, 250, 'characters'],
  ['Explorateur', 'Découvrir 5 personnages.', 5, 200, 'characters'],
  ['Collectionneur', 'Garder une série de 7 jours.', 7, 150, 'streak'],
]) seedQuest.run(...quest);

// Create official admin account
const adminUser = db.prepare(`
  SELECT id FROM users WHERE username = 'chat ile_official'
`).get();

if (!adminUser) {
  const adminStmt = db.prepare(`
    INSERT INTO users (device_id, username, display_name, bio, pieces, role, is_verified, badge_type, is_system_account, created_at) 
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
  `);
  adminStmt.run(
    'admin_device_' + Date.now(),
    'chat ile_official',
    'CHATILE Officiel',
    'Compte officiel de CHATILE. Mises à jour, annonces, et feedbacks. On lit tout. 💜',
    999999,
    'admin',
    1,
    'official',
    1
  );
  
  // Create profile for admin
  const adminId = db.prepare('SELECT id FROM users WHERE username = ?').get('chat ile_official').id;
  db.prepare(`
    INSERT OR IGNORE INTO profiles (user_id, display_name, bio) 
    VALUES (?, ?, ?)
  `).run(adminId, 'CHATILE Officiel', 'Compte officiel de CHATILE. Mises à jour, annonces, et feedbacks. On lit tout. 💜');
}

// Create moderator account
const modUser = db.prepare(`
  SELECT id FROM users WHERE username = 'chat ile_mod'
`).get();

if (!modUser) {
  const modStmt = db.prepare(`
    INSERT INTO users (device_id, username, display_name, bio, pieces, role, is_verified, badge_type, is_system_account, created_at) 
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
  `);
  modStmt.run(
    'mod_device_' + Date.now(),
    'chat ile_mod',
    'Équipe Modération',
    "Veille à la santé de la communauté. Signale un contenu, on s'en occupe.",
    999999,
    'moderator',
    1,
    'moderator',
    1
  );
  
  // Create profile for moderator
  const modId = db.prepare('SELECT id FROM users WHERE username = ?').get('chat ile_mod').id;
  db.prepare(`
    INSERT OR IGNORE INTO profiles (user_id, display_name, bio) 
    VALUES (?, ?, ?)
  `).run(modId, 'Équipe Modération', "Veille à la santé de la communauté. Signale un contenu, on s'en occupe.");
}

const app = express();
app.disable('x-powered-by');
app.use(express.json({ limit: '32kb' }));
const uploadDirectory = resolve(__dirname, 'uploads');
mkdirSync(uploadDirectory, { recursive: true });
const pendingCharacterImages = new Set();
app.use('/uploads', express.static(uploadDirectory, { fallthrough: false, maxAge: '1d' }));
const avatarUpload = multer({
  dest: uploadDirectory,
  limits: { fileSize: 5 * 1024 * 1024, files: 1 },
  fileFilter: (req, file, callback) => {
    callback(null, ['image/jpeg', 'image/png', 'image/webp'].includes(file.mimetype));
  },
});

if (admin.apps.length === 0 && (
  process.env.FIREBASE_PROJECT_ID || process.env.FIREBASE_SERVICE_ACCOUNT || process.env.GOOGLE_APPLICATION_CREDENTIALS
)) {
  const serviceAccount = process.env.FIREBASE_SERVICE_ACCOUNT
    ? JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)
    : null;
  admin.initializeApp({
    credential: serviceAccount ? admin.credential.cert(serviceAccount) : admin.credential.applicationDefault(),
    ...(process.env.FIREBASE_PROJECT_ID ? { projectId: process.env.FIREBASE_PROJECT_ID } : {}),
  });
}

function getOrCreateFirebaseUser(decoded) {
  let user = db.prepare('SELECT * FROM users WHERE firebase_uid = ?').get(decoded.uid);
  const targetAdminEmail = process.env.ADMIN_EMAIL || 'matthias.duprey1@gmail.com';
  const isAdmin = decoded.email && decoded.email.toLowerCase() === targetAdminEmail.toLowerCase();
  const role = isAdmin ? 'admin' : 'user';

  if (!user) {
    const result = db.prepare(
      `INSERT INTO users (device_id, firebase_uid, display_name, avatar_url, pieces, welcome_bonus_claimed, role, created_at)
       VALUES (?, ?, ?, ?, 1000, 1, ?, CURRENT_TIMESTAMP)`
    ).run(`firebase_${decoded.uid}`, decoded.uid, decoded.name || 'Utilisateur CHATILE', decoded.picture || null, role);
    user = db.prepare('SELECT * FROM users WHERE id = ?').get(result.lastInsertRowid);
    db.prepare('INSERT INTO profiles (user_id, display_name) VALUES (?, ?)')
      .run(user.id, decoded.name || 'Utilisateur CHATILE');
  } else {
    const newRole = isAdmin ? 'admin' : user.role;
    if (newRole !== user.role || (!user.display_name?.trim() && decoded.name?.trim())) {
      db.prepare("UPDATE users SET role = ?, display_name = COALESCE(NULLIF(display_name, ''), ?), avatar_url = COALESCE(avatar_url, ?) WHERE id = ?")
        .run(newRole, decoded.name?.trim() || user.display_name, decoded.picture || null, user.id);
      user = db.prepare('SELECT * FROM users WHERE id = ?').get(user.id);
    }
  }
  return user;
}

async function identifyUser(req, res, next) {
  if (req.path === '/auth/firebase') return next();
  const authorization = req.get('authorization') ?? '';
  if (!authorization.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Authentification requise', needLogin: true });
  }
  if (!admin.apps.length) {
    return res.status(503).json({ error: 'Firebase Auth n’est pas configuré.' });
  }

  let decoded;
  try {
    decoded = await admin.auth().verifyIdToken(authorization.slice(7).trim());
  } catch (error) {
    console.error('Firebase ID token verification failed:', error.code, error.message);
    return res.status(401).json({ error: 'Token Firebase invalide', needLogin: true });
  }
  let user;
  try {
    user = getOrCreateFirebaseUser(decoded);
  } catch (error) {
    console.error('Firebase user sync failed:', error.message);
    return next(error);
  }

  if (user.banned_until && Date.parse(user.banned_until) > Date.now()) {
    return res.status(403).json({ error: 'Compte suspendu.', bannedUntil: user.banned_until });
  }

  if (['premium', 'premium_plus'].includes(user.subscription_tier) && user.subscription_expires_at &&
      Date.parse(user.subscription_expires_at) <= Date.now()) {
    db.prepare("UPDATE users SET subscription_tier = 'free' WHERE id = ?").run(user.id);
    user = db.prepare('SELECT * FROM users WHERE id = ?').get(user.id);
  }
  req.user = user;
  return next();
}

app.use('/api', identifyUser);

function getFallbackReply(character, message) {
  const greeting = character?.greeting || 'Bonjour !';
  const responses = [
    `${greeting} C'est très intéressant ce que tu me dis à propos de "${message}". Dis-m'en plus !`,
    `Je vois... C'est fascinant ! Que penses-tu qu'on devrait faire ensuite ?`,
    `Hmm, je comprends tout à fait ton point de vue sur "${message}". Raconte-moi une autre anecdote.`,
    `C'est une excellente remarque ! Je suis toujours là pour t'écouter et discuter avec toi.`,
    `Absolument ! Qu'aimerais-tu explorer d'autre ensemble ?`,
  ];
  return responses[Math.floor(Math.random() * responses.length)];
}

async function callMistral(messages, nsfwEnabled, model = 'mistral-small-2603') {
  if (!process.env.MISTRAL_API_KEY) throw new Error('MISTRAL_API_KEY_NOT_CONFIGURED');

  const response = await fetch('https://api.mistral.ai/v1/chat/completions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${process.env.MISTRAL_API_KEY}`
    },
    body: JSON.stringify({
      model,
      messages,
      max_tokens: 500,
    }),
    signal: AbortSignal.timeout(45_000),
  });

  if (response.status === 401) {
    console.error('Clé API Mistral invalide ou révoquée');
    throw new Error('MISTRAL_INVALID_API_KEY');
  }
  if (response.status === 403) {
    throw new Error('BLOCKED_BY_GUARDRAIL');
  }
  if (response.status === 429) {
    console.error('Rate limit Mistral atteint');
    throw new Error('MISTRAL_RATE_LIMIT_EXCEEDED');
  }
  if (response.status >= 500) {
    console.error('Erreur serveur Mistral');
    throw new Error('MISTRAL_SERVER_ERROR');
  }
  if (!response.ok) {
    const detail = await response.text();
    console.error('Mistral request failed:', response.status, detail.slice(0, 500));
    throw new Error('MISTRAL_REQUEST_FAILED');
  }

  const data = await response.json();
  const text = data.choices?.[0]?.message?.content;
  if (typeof text !== 'string' || !text.trim()) throw new Error('EMPTY_MISTRAL_RESPONSE');
  return text.trim();
}

function parseFacts(summary) {
  try {
    const parsed = JSON.parse(summary);
    return Array.isArray(parsed.facts) ? parsed.facts.filter((fact) => typeof fact === 'string') : [];
  } catch {
    return [];
  }
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (character) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  })[character]);
}

async function extractMemory(characterId, userId, conversationText, nsfwEnabled) {
  try {
    const result = await callMistral([
      {
        role: 'system',
        content: 'Extract at most 5 durable facts explicitly stated by the user; never infer. Classify mood as positive, neutral, or intimate. Optionally extract structured facts under identity, preferences, relationships, events, emotions as arrays of {"key":"...","value":"...","confidence":0.0}. Return only JSON: {"facts":["..."],"mood":"neutral","structured":{}}.',
      },
      { role: 'user', content: conversationText.slice(-12_000) },
    ], nsfwEnabled);
    const parsed = JSON.parse(result);
    if (Array.isArray(parsed.facts)) {
      const existing = db.prepare('SELECT fact FROM learned_memory WHERE character_id = ? AND user_id = ?')
        .all(characterId, userId).map((row) => row.fact);
      const seen = new Set(existing.map((fact) => fact.trim().toLocaleLowerCase()));
      for (const candidate of parsed.facts.slice(0, 5)) {
        if (typeof candidate !== 'string') continue;
        const fact = candidate.trim().slice(0, 300);
        const key = fact.toLocaleLowerCase();
        if (fact && !seen.has(key) && existing.length < 30) {
          db.prepare('INSERT INTO learned_memory (character_id, user_id, fact) VALUES (?, ?, ?)')
            .run(characterId, userId, fact);
          existing.push(fact);
          seen.add(key);
        }
      }
    }
    const mood = ['positive', 'neutral', 'intimate'].includes(parsed.mood) ? parsed.mood : null;
    db.prepare(`INSERT INTO character_state (character_id, user_id, relationship_level, last_mood)
      VALUES (?, ?, 0, ?) ON CONFLICT(character_id, user_id) DO UPDATE SET last_mood = excluded.last_mood`)
      .run(characterId, userId, mood ?? 'neutral');
    const owner = db.prepare('SELECT subscription_tier FROM users WHERE id = ?').get(userId);
    if (owner?.subscription_tier === 'premium_plus' && parsed.structured && typeof parsed.structured === 'object') {
      const addStructured = db.prepare(`INSERT INTO structured_memory (character_id, user_id, category, key, value, confidence)
        VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT(character_id, user_id, category, key) DO UPDATE SET
        value = excluded.value, confidence = excluded.confidence`);
      for (const [category, entries] of Object.entries(parsed.structured)) {
        if (!['identity', 'preferences', 'relationships', 'events', 'emotions'].includes(category) || !Array.isArray(entries)) continue;
        for (const entry of entries.slice(0, 10)) {
          if (typeof entry?.key !== 'string' || typeof entry?.value !== 'string') continue;
          addStructured.run(characterId, userId, category, entry.key.slice(0, 80), entry.value.slice(0, 300),
            Math.min(1, Math.max(0, Number(entry.confidence) || 0.5)));
        }
      }
    }
    const facts = db.prepare('SELECT fact FROM learned_memory WHERE character_id = ? AND user_id = ? ORDER BY id DESC LIMIT 30')
      .all(characterId, userId).reverse().map((row) => row.fact);
    db.prepare(`INSERT INTO character_memory (character_id, user_id, summary) VALUES (?, ?, ?)
      ON CONFLICT(character_id, user_id) DO UPDATE SET summary = excluded.summary`)
      .run(characterId, userId, JSON.stringify({ facts, mood: mood ?? 'neutral' }));
    addCharacterJournal(characterId, userId, 'memory_updated', 'La mémoire de la conversation a été mise à jour.');
  } catch (error) {
    console.error('Memory extraction failed:', error.message);
  }
}

function ownedCharacter(characterId, userId) {
  return db.prepare('SELECT * FROM characters WHERE id = ? AND (user_id = ? OR is_system = 1)')
    .get(characterId, userId);
}

function recordPieceTransaction(userId, amount, type, description) {
  db.prepare('INSERT INTO piece_transactions (user_id, amount, type, description) VALUES (?, ?, ?, ?)')
    .run(userId, amount, type, description);
}

function utcDate(date = new Date()) {
  return date.toISOString().slice(0, 10);
}

function weekStart(date = new Date()) {
  const monday = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
  monday.setUTCDate(monday.getUTCDate() - ((monday.getUTCDay() + 6) % 7));
  return utcDate(monday);
}

function ensureUserQuests(userId) {
  const currentWeek = weekStart();
  const insert = db.prepare('INSERT OR IGNORE INTO user_quests (user_id, quest_id, week_start) VALUES (?, ?, ?)');
  for (const quest of db.prepare('SELECT id FROM weekly_quests').all()) insert.run(userId, quest.id, currentWeek);
  return currentWeek;
}

function updateQuestProgress(userId, type, amount = 1) {
  const currentWeek = ensureUserQuests(userId);
  db.prepare(`UPDATE user_quests SET progress = MIN(
      (SELECT target FROM weekly_quests WHERE id = user_quests.quest_id), progress + ?)
    WHERE user_id = ? AND week_start = ? AND quest_id IN (SELECT id FROM weekly_quests WHERE type = ?)`)
    .run(amount, userId, currentWeek, type);
}

function addCharacterJournal(characterId, userId, eventType, description) {
  db.prepare(`INSERT INTO character_journal (character_id, user_id, event_type, description)
    VALUES (?, ?, ?, ?)`).run(characterId, userId, eventType, description);
}

function ensureReferralCode(userId) {
  const user = db.prepare('SELECT referral_code FROM users WHERE id = ?').get(userId);
  if (user?.referral_code) return user.referral_code;
  const code = randomUUID().replaceAll('-', '').slice(0, 10).toUpperCase();
  db.prepare('UPDATE users SET referral_code = ? WHERE id = ? AND referral_code IS NULL').run(code, userId);
  return db.prepare('SELECT referral_code FROM users WHERE id = ?').get(userId)?.referral_code ?? code;
}

async function generateCharacterImage(characterId, name, prompt) {
  pendingCharacterImages.add(characterId);
  try {
    let avatarUrl;
    const imagePrompt = `Create a square profile portrait for a fictional character named ${name}. Visual brief: ${prompt.slice(0, 1000)}. No text, no logos.`;
    if (process.env.OPENAI_API_KEY) {
      const response = await fetch('https://api.openai.com/v1/images/generations', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${process.env.OPENAI_API_KEY}`,
        },
        body: JSON.stringify({
          model: process.env.OPENAI_IMAGE_MODEL || 'gpt-image-1',
          prompt: imagePrompt,
          size: '1024x1024',
          output_format: 'png',
        }),
        signal: AbortSignal.timeout(60_000),
      });
      if (response.ok) {
        const result = await response.json();
        const imageData = result.data?.[0]?.b64_json;
        if (typeof imageData === 'string') {
          const filename = `character-${characterId}.png`;
          const imageDirectory = resolve(uploadDirectory, 'characters');
          mkdirSync(imageDirectory, { recursive: true });
          writeFileSync(resolve(imageDirectory, filename), Buffer.from(imageData, 'base64'));
          const publicBaseUrl = (process.env.PUBLIC_BASE_URL || 'http://10.0.2.2:3000').replace(/\/+$/, '');
          avatarUrl = `${publicBaseUrl}/uploads/characters/${filename}`;
        }
      }
    } else if (process.env.IMAGE_GENERATION_URL) {
      const response = await fetch(process.env.IMAGE_GENERATION_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(process.env.IMAGE_GENERATION_API_KEY
            ? { Authorization: `Bearer ${process.env.IMAGE_GENERATION_API_KEY}` } : {}),
        },
        body: JSON.stringify({ prompt: imagePrompt, characterId }),
        signal: AbortSignal.timeout(60_000),
      });
      if (response.ok) {
        const result = await response.json();
        if (typeof result.url === 'string' && /^https?:\/\//.test(result.url)) avatarUrl = result.url;
      }
    }

    if (!avatarUrl) {
      const seed = encodeURIComponent(name.trim() || 'character');
      avatarUrl = `https://api.dicebear.com/7.x/avataaars/png?seed=${seed}&backgroundColor=b6e3f4,c0aede,d1d4f9,ffd5dc,ffdfbf`;
    }

    db.prepare('UPDATE characters SET avatar_url = ?, is_public = 1 WHERE id = ? AND is_system = 0')
      .run(avatarUrl, characterId);
  } catch (error) {
    console.error('Character image generation failed:', error.message);
    const seed = encodeURIComponent(name.trim() || 'character');
    const fallbackUrl = `https://api.dicebear.com/7.x/avataaars/png?seed=${seed}`;
    db.prepare('UPDATE characters SET avatar_url = ?, is_public = 1 WHERE id = ? AND is_system = 0')
      .run(fallbackUrl, characterId);
  } finally {
    pendingCharacterImages.delete(characterId);
  }
}

function buildSystemPrompt(character, userId) {
  if (!character) return 'Tu es un assistant conversationnel chaleureux, utile et concis.';
  const seedFacts = db.prepare('SELECT fact FROM seed_memory WHERE character_id = ? AND user_id = ? ORDER BY id LIMIT 30')
    .all(character.id, userId).map((row) => row.fact);
  const learnedFacts = db.prepare('SELECT fact FROM learned_memory WHERE character_id = ? AND user_id = ? ORDER BY id DESC LIMIT 30')
    .all(character.id, userId).reverse().map((row) => row.fact);
  const pinned = db.prepare('SELECT message_text FROM pinned_messages WHERE character_id = ? AND user_id = ? ORDER BY id DESC LIMIT 10')
    .all(character.id, userId).map((row) => row.message_text);
  const state = db.prepare('SELECT relationship_level, last_mood FROM character_state WHERE character_id = ? AND user_id = ?')
    .get(character.id, userId);
  const events = db.prepare(`SELECT label FROM character_events WHERE character_id = ? AND user_id = ?
    AND (event_date = ? OR (recurring = 1 AND substr(event_date, 6) = ?)) ORDER BY id DESC LIMIT 5`)
    .all(character.id, userId, utcDate(), utcDate().slice(5));
  const moodJournal = db.prepare(`SELECT mood, note FROM mood_journal WHERE user_id = ?
    AND created_at >= date('now') ORDER BY id DESC LIMIT 1`).get(userId);
  const account = db.prepare('SELECT subscription_tier FROM users WHERE id = ?').get(userId);
  const structured = account?.subscription_tier === 'premium_plus'
    ? db.prepare('SELECT category, key, value FROM structured_memory WHERE character_id = ? AND user_id = ? ORDER BY category, key')
      .all(character.id, userId)
    : [];
  const prompt = [character.system_prompt];
  if (seedFacts.length) prompt.push(`Ce que tu sais déjà sur l’utilisateur (fourni à ta création) :\n- ${seedFacts.join('\n- ')}`);
  if (learnedFacts.length) prompt.push(`Ce que tu as appris au fil des conversations :\n- ${learnedFacts.join('\n- ')}`);
  if (pinned.length) prompt.push(`Messages épinglés importants :\n- ${pinned.join('\n- ')}`);
  prompt.push(`Ton niveau de relation : ${state?.relationship_level ?? 0}/10.\nTon humeur actuelle : ${state?.last_mood ?? character.last_mood ?? 'neutral'}.`);
  if (character.appearance) prompt.push(`Ton apparence physique (fixe) : ${character.appearance}.`);
  if (character.banned_words) prompt.push(`Mots interdits : ${character.banned_words}.`);
  if (character.scenario_goal) prompt.push(`Objectif de cette scène : ${character.scenario_goal}. Termine la scène quand l’objectif est atteint.`);
  if (character.boundaries) prompt.push(`Tes limites : ${character.boundaries}. Exprime-les poliment si nécessaire.`);
  if (events.length) prompt.push(`Aujourd’hui c’est ${events.map((row) => row.label).join(', ')}. Mentionne-le naturellement.`);
  if (character.scenario_steps) prompt.push(`Étapes du scénario : ${character.scenario_steps}.`);
  if (moodJournal) prompt.push(`L’utilisateur a indiqué se sentir ${moodJournal.mood}${moodJournal.note ? ` : ${moodJournal.note}` : ''}. Réponds avec tact.`);
  if (structured.length) prompt.push(`Mémoire structurée :\n${structured.map((item) => `${item.category} · ${item.key} : ${item.value}`).join('\n')}`);
  return prompt.join('\n\n');
}

function attributeBadge(user) {
  // Badge hierarchy: premium_plus > premium > creator > official > moderator
  if (user.badge_type) return user.badge_type;
  
  if (user.role === "admin") return "official";
  if (user.role === "moderator") return "moderator";
  
  if (user.subscription_tier === "premium_plus") return "premium_plus";
  if (user.subscription_tier === "premium") return "premium";
  
  // Check if user is a creator (3+ public characters with 50+ likes)
  const publicChars = db.prepare(`
    SELECT COUNT(*) as count FROM characters 
    WHERE user_id = ? AND is_public = 1 AND is_system = 0 AND likes_count >= 50
  `).get(user.id);
  
  if (publicChars && publicChars.count >= 3) return "creator";
  
  return null;
}

app.get('/api/me', (req, res) => {
  const badge = getUserBadge(req.user);
  const today = utcDate();
  const currentWeekStart = weekStart();
  
  // Daily claim status
  const lastDailyClaim = req.user.last_daily_claim?.slice(0, 10) ?? null;
  const canClaimDaily = lastDailyClaim !== today;
  const nextDailyClaimAt = canClaimDaily ? null : (() => {
    const tomorrow = new Date(`${today}T00:00:00.000Z`);
    tomorrow.setUTCDate(tomorrow.getUTCDate() + 1);
    return tomorrow.toISOString();
  })();
  
  // Mystery box status
  const lastMysteryBox = req.user.last_mystery_box?.slice(0, 10) ?? null;
  const canOpenMysteryBox = !lastMysteryBox || lastMysteryBox < currentWeekStart;
  const nextMysteryBoxAt = canOpenMysteryBox ? null : (() => {
    const nextMonday = new Date(`${currentWeekStart}T00:00:00.000Z`);
    nextMonday.setUTCDate(nextMonday.getUTCDate() + 7);
    return nextMonday.toISOString();
  })();
  
  // Watch ad status
  const lastWatchAd = req.user.last_watch_ad ? Date.parse(`${req.user.last_watch_ad.replace(' ', 'T')}Z`) : 0;
  const canWatchAd = !lastWatchAd || Date.now() - lastWatchAd >= 60 * 60 * 1000;
  const nextWatchAdAt = canWatchAd ? null : new Date(lastWatchAd + 60 * 60 * 1000).toISOString();
  
  res.json({
    pieces: req.user.pieces,
    nsfw_enabled: Boolean(req.user.nsfw_enabled),
    subscription_tier: req.user.subscription_tier,
    subscription_expires_at: req.user.subscription_expires_at,
    messages_since_last_ad: req.user.messages_since_last_ad,
    messages_until_next_ad: Math.max(0, 50 - req.user.messages_since_last_ad),
    badge_type: badge,
    username: req.user.username,
    display_name: req.user.display_name,
    avatar_url: req.user.avatar_url,
    streak_days: req.user.streak_days,
    tutorial_completed: Boolean(req.user.tutorial_completed),
    theme: req.user.theme,
    canClaimDaily,
    nextDailyClaimAt,
    canOpenMysteryBox,
    nextMysteryBoxAt,
    canWatchAd,
    nextWatchAdAt,
  });
});

app.get('/api/profile', (req, res) => {
  db.prepare(`INSERT OR IGNORE INTO profiles (user_id, display_name, bio, avatar_url)
    SELECT id, display_name, bio, avatar_url FROM users WHERE id = ?`).run(req.user.id);
  const profile = db.prepare(
    'SELECT display_name AS displayName, bio, avatar_url AS avatarUrl FROM profiles WHERE user_id = ?',
  ).get(req.user.id);
  res.json({
    ...(profile || {}),
    role: req.user.role,
  });
});

app.put('/api/profile', (req, res) => {
  const displayName = typeof req.body?.displayName === 'string' ? req.body.displayName.trim() : null;
  const bio = typeof req.body?.bio === 'string' ? req.body.bio.trim() : null;
  if (displayName === null || bio === null || displayName.length > 80 || bio.length > 500) {
    return res.status(400).json({ error: 'Profil invalide.' });
  }
  const update = db.transaction(() => {
    db.prepare(`INSERT OR IGNORE INTO profiles (user_id) VALUES (?)`).run(req.user.id);
    db.prepare('UPDATE profiles SET display_name = ?, bio = ? WHERE user_id = ?').run(displayName, bio, req.user.id);
    db.prepare('UPDATE users SET display_name = ?, bio = ? WHERE id = ?').run(displayName, bio, req.user.id);
  });
  update();
  res.json({ displayName, bio });
});

app.post('/api/auth/firebase', async (req, res) => {
  const { idToken } = req.body;
  if (!idToken) return res.status(400).json({ error: 'idToken requis' });
  if (!admin.apps.length) return res.status(503).json({ error: 'Firebase Auth n’est pas configuré.' });
  let decoded;
  try {
    decoded = await admin.auth().verifyIdToken(idToken);
  } catch (error) {
    console.error('Firebase ID token verification failed:', error.code, error.message);
    return res.status(401).json({ error: 'Token Firebase invalide' });
  }

  try {
    const user = getOrCreateFirebaseUser(decoded);
    if (user.banned_until && Date.parse(user.banned_until) > Date.now()) {
      return res.status(403).json({ error: 'Compte suspendu.', bannedUntil: user.banned_until });
    }

    res.json({
      pieces: user.pieces,
      nsfw_enabled: user.nsfw_enabled,
      subscription_tier: user.subscription_tier,
      badge_type: user.badge_type,
      display_name: user.display_name
    });
  } catch (error) {
    console.error('Firebase user sync failed:', error.message);
    res.status(500).json({ error: 'Impossible de synchroniser le compte Firebase.' });
  }
});

app.post('/api/profile/avatar', (req, res) => {
  avatarUpload.single('avatar')(req, res, (error) => {
    if (error) return res.status(400).json({ error: 'Image invalide (JPEG, PNG ou WebP, 5 Mo max).' });
    if (!req.file) return res.status(400).json({ error: 'Aucune image valide fournie.' });
    const extension = ({ 'image/jpeg': '.jpg', 'image/png': '.png', 'image/webp': '.webp' })[req.file.mimetype];
    const filename = `${req.file.filename}${extension}`;
    try {
      renameSync(req.file.path, resolve(uploadDirectory, filename));
    } catch {
      return res.status(500).json({ error: 'Impossible d’enregistrer l’image.' });
    }
    const avatarUrl = `${process.env.PUBLIC_BASE_URL ?? 'http://10.0.2.2:3000'}/uploads/${filename}`;
    db.prepare('UPDATE users SET avatar_url = ? WHERE id = ?').run(avatarUrl, req.user.id);
    db.prepare(`INSERT INTO profiles (user_id, avatar_url) VALUES (?, ?)
      ON CONFLICT(user_id) DO UPDATE SET avatar_url = excluded.avatar_url`).run(req.user.id, avatarUrl);
    return res.json({ avatar_url: avatarUrl });
  });
});

function dailyClaimState(user) {
  const today = utcDate();
  const lastClaimDate = user.last_daily_claim?.slice(0, 10) ?? null;
  const yesterday = utcDate(new Date(Date.now() - 24 * 60 * 60 * 1000));
  const nextClaimAt = new Date(`${today}T00:00:00.000Z`);
  nextClaimAt.setUTCDate(nextClaimAt.getUTCDate() + 1);
  return {
    canClaim: lastClaimDate !== today,
    nextClaimAt: lastClaimDate === today ? nextClaimAt.toISOString() : null,
    streak_days: lastClaimDate === yesterday ? user.streak_days + 1 : lastClaimDate === today ? user.streak_days : 1,
    todayReward: Math.min(80, 20 + 10 * (lastClaimDate === yesterday ? user.streak_days : 0)),
  };
}

app.get('/api/daily-status', (req, res) => res.json(dailyClaimState(req.user)));

app.post('/api/daily-claim', (req, res) => {
  const claim = db.transaction(() => {
    const user = db.prepare('SELECT * FROM users WHERE id = ?').get(req.user.id);
    const today = utcDate();
    const lastClaimDate = user.last_daily_claim?.slice(0, 10) ?? null;
    
    if (lastClaimDate === today) {
      const tomorrow = new Date(`${today}T00:00:00.000Z`);
      tomorrow.setUTCDate(tomorrow.getUTCDate() + 1);
      return { 
        success: false, 
        error: 'already_claimed_today', 
        nextClaimAt: tomorrow.toISOString(), 
        message: "Tu as déjà réclamé ta récompense aujourd'hui. Reviens demain !" 
      };
    }
    
    const yesterday = utcDate(new Date(Date.now() - 24 * 60 * 60 * 1000));
    const streak = user.last_streak_date === yesterday ? user.streak_days + 1 : 1;
    const reward = Math.min(80, 20 + 10 * (streak - 1));
    
    db.prepare(`UPDATE users SET pieces = pieces + ?, last_daily_claim = ?, last_streak_date = ?, streak_days = ?
      WHERE id = ?`).run(reward, `${today} 00:00:00`, today, streak, req.user.id);
    recordPieceTransaction(req.user.id, reward, 'daily_claim', `Récompense quotidienne · série ${streak} jours`);
    updateQuestProgress(req.user.id, 'streak', streak);
    const updated = db.prepare('SELECT pieces FROM users WHERE id = ?').get(req.user.id);
    return { success: true, pieces: updated.pieces, streak_days: streak, reward };
  })();
  res.json(claim);
});

app.get('/api/shop', (req, res) => {
  res.json({ items: [
    { id: 'pack_500', name: 'Pack 500 Pièces', pieces: 500, price: '2,00 €', productId: 'pack_500', type: 'consumable' },
    { id: 'pack_1500', name: 'Pack 1 500 Pièces', pieces: 1500, price: '5,00 €', productId: 'pack_1500', type: 'consumable' },
    { id: 'pack_5000', name: 'Pack 5 000 Pièces', pieces: 5000, price: '15,00 €', productId: 'pack_5000', type: 'consumable' },
    { id: 'pack_15000', name: 'Pack 15 000 Pièces', pieces: 15000, price: '40,00 €', productId: 'pack_15000', type: 'consumable' },
    { id: 'sub_premium_monthly', name: 'Premium', pieces: 4000, price: '7,99 €/mois', productId: 'sub_premium_monthly', type: 'subscription' },
    { id: 'sub_premium_plus_monthly', name: 'Premium+', pieces: 7000, price: '14,99 €/mois', productId: 'sub_premium_plus_monthly', type: 'subscription' },
  ] });
});

app.post('/api/verify-purchase', async (req, res) => {
  const purchaseToken = typeof req.body?.purchaseToken === 'string' ? req.body.purchaseToken.trim() : '';
  const productId = typeof req.body?.productId === 'string' ? req.body.productId.trim() : '';
  const product = {
    pack_500: { pieces: 500, type: 'product' },
    pack_1500: { pieces: 1500, type: 'product' },
    pack_5000: { pieces: 5000, type: 'product' },
    pack_15000: { pieces: 15000, type: 'product' },
    pack_500_pieces: { pieces: 500, type: 'product' },
    sub_premium_monthly: { pieces: 4000, type: 'subscription', tier: 'premium' },
    sub_premium_plus_monthly: { pieces: 7000, type: 'subscription', tier: 'premium_plus' },
  }[productId];
  if (!purchaseToken || purchaseToken.length > 4096 || !product) {
    return res.status(400).json({ error: 'purchaseToken invalide.' });
  }

  const existing = db.prepare('SELECT user_id, product_id FROM purchases WHERE token = ?').get(purchaseToken);
  if (existing && existing.user_id !== req.user.id) {
    return res.status(409).json({ error: 'Ce paiement est déjà associé à un autre compte.' });
  }
  if (existing && existing.product_id && existing.product_id !== productId) {
    return res.status(409).json({ error: 'Le jeton ne correspond pas à ce produit.' });
  }

  try {
    if (!process.env.GOOGLE_PLAY_PACKAGE_NAME || !process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      return res.status(503).json({ error: 'La vérification Google Play n’est pas configurée.' });
    }
    const auth = new GoogleAuth({ scopes: ['https://www.googleapis.com/auth/androidpublisher'] });
    const androidPublisher = google.androidpublisher({ version: 'v3', auth });
    const packageName = process.env.GOOGLE_PLAY_PACKAGE_NAME;
    let expiryTime = null;
    let subscriptionOrderId = null;
    if (product.type === 'subscription') {
      const subscription = await androidPublisher.purchases.subscriptionsv2.get({
        packageName,
        token: purchaseToken,
      });
      const item = subscription.data.lineItems?.find((line) => line.productId === productId);
      expiryTime = item?.expiryTime ?? null;
      subscriptionOrderId = subscription.data.latestOrderId ?? null;
      const active = ['SUBSCRIPTION_STATE_ACTIVE', 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD']
        .includes(subscription.data.subscriptionState);
      if (!active || !expiryTime || Date.parse(expiryTime) <= Date.now()) {
        return res.status(400).json({ error: 'L’abonnement Google Play n’est pas actif.' });
      }
      if (!existing && subscription.data.acknowledgementState !== 'ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED') {
        await androidPublisher.purchases.subscriptions.acknowledge({
          packageName,
          subscriptionId: productId,
          token: purchaseToken,
          requestBody: {},
        });
      }
    } else {
      const purchase = await androidPublisher.purchases.products.get({
        packageName, productId, token: purchaseToken,
      });
      if (purchase.data.purchaseState !== 0) {
        return res.status(400).json({ error: 'Le paiement Google Play n’est pas valide.' });
      }
      if (purchase.data.acknowledgementState !== 1) {
        await androidPublisher.purchases.products.acknowledge({
          packageName, productId, token: purchaseToken, requestBody: {},
        });
      }
    }

    if (!existing) {
      const applyPurchase = db.transaction(() => {
        db.prepare(`INSERT INTO purchases (token, user_id, product_id, purchase_type)
          VALUES (?, ?, ?, ?)`).run(purchaseToken, req.user.id, productId, product.type);
        if (product.type === 'subscription') {
          const orderId = subscriptionOrderId ?? purchaseToken;
          const credit = db.prepare('INSERT OR IGNORE INTO subscription_credits (order_id, purchase_token, user_id) VALUES (?, ?, ?)')
            .run(orderId, purchaseToken, req.user.id);
          db.prepare(`UPDATE users SET pieces = pieces + ?, subscription_tier = ?,
            subscription_expires_at = ? WHERE id = ?`).run(credit.changes ? product.pieces : 0, product.tier, expiryTime, req.user.id);
          if (credit.changes) recordPieceTransaction(req.user.id, product.pieces, 'subscription', `${product.tier} · mensualité`);
        } else {
          db.prepare('UPDATE users SET pieces = pieces + ? WHERE id = ?').run(product.pieces, req.user.id);
          recordPieceTransaction(req.user.id, product.pieces, 'purchase', productId);
        }
      });
      applyPurchase();
    } else if (product.type === 'subscription') {
      const applyRenewal = db.transaction(() => {
        const orderId = subscriptionOrderId;
        const credit = orderId
          ? db.prepare('INSERT OR IGNORE INTO subscription_credits (order_id, purchase_token, user_id) VALUES (?, ?, ?)')
            .run(orderId, purchaseToken, req.user.id)
          : { changes: 0 };
        db.prepare(`UPDATE users SET pieces = pieces + ?, subscription_tier = ?,
          subscription_expires_at = ? WHERE id = ?`).run(credit.changes ? product.pieces : 0, product.tier, expiryTime, req.user.id);
        if (credit.changes) recordPieceTransaction(req.user.id, product.pieces, 'subscription', `${product.tier} · mensualité`);
      });
      applyRenewal();
    }

    const updated = db.prepare('SELECT pieces, subscription_tier FROM users WHERE id = ?').get(req.user.id);
    res.json({ pieces: updated.pieces, subscription_tier: updated.subscription_tier });
  } catch (error) {
    if (error.code === 'SQLITE_CONSTRAINT_UNIQUE') {
      return res.status(409).json({ error: 'Ce paiement a déjà été traité.' });
    }
    console.error('Google Play verification failed:', error.message);
    res.status(502).json({ error: 'Impossible de vérifier le paiement auprès de Google Play.' });
  }
});

app.post('/api/watch-ad', (req, res) => {
  if (['premium', 'premium_plus'].includes(req.user.subscription_tier)) {
    return res.status(403).json({ error: 'Les publicités ne sont pas disponibles avec votre abonnement.' });
  }
  const claim = db.transaction(() => {
    const adCountToday = db.prepare(`
      SELECT COUNT(*) AS count FROM piece_transactions
      WHERE user_id = ? AND type = 'watch_ad' AND date(created_at) = date('now')
    `).get(req.user.id).count;
    if (adCountToday >= 10) {
      return { success: false, error: 'daily_limit_reached', message: 'Maximum de 10 publicités par jour atteint pour les pièces.' };
    }

    const user = db.prepare('SELECT last_watch_ad FROM users WHERE id = ?').get(req.user.id);
    const last = user.last_watch_ad ? Date.parse(`${user.last_watch_ad.replace(' ', 'T')}Z`) : 0;
    if (last && Date.now() - last < 5 * 60 * 1000) {
      return { success: false, error: 'too_soon', nextClaimAt: new Date(last + 5 * 60 * 1000).toISOString(), message: 'Attends encore 5 minutes avant de regarder une autre publicité.' };
    }
    const awardedAt = new Date().toISOString().replace('T', ' ').slice(0, 19);
    db.prepare('UPDATE users SET pieces = pieces + 5, last_watch_ad = ? WHERE id = ?').run(awardedAt, req.user.id);
    recordPieceTransaction(req.user.id, 5, 'watch_ad', 'Publicité récompensée (+5 Pièces)');
    return { success: true };
  })();
  if (!claim.success) {
    return res.status(429).json(claim);
  }
  const updated = db.prepare('SELECT pieces FROM users WHERE id = ?').get(req.user.id);
  res.json({ success: true, pieces: updated.pieces });
});

app.post('/api/mystery-box', (req, res) => {
  const claim = db.transaction(() => {
    const user = db.prepare('SELECT last_mystery_box FROM users WHERE id = ?').get(req.user.id);
    const currentWeekStart = weekStart();
    const lastClaimDate = user.last_mystery_box?.slice(0, 10) ?? null;
    if (lastClaimDate && lastClaimDate >= currentWeekStart) {
      const nextMonday = new Date(`${currentWeekStart}T00:00:00.000Z`);
      nextMonday.setUTCDate(nextMonday.getUTCDate() + 7);
      return { success: false, nextClaimAt: nextMonday.toISOString() };
    }

    const amount = 10 + Math.floor(Math.random() * 41);
    const rarity = amount <= 20 ? 'commun' : amount <= 35 ? 'rare' : 'épique';
    db.prepare('UPDATE users SET pieces = pieces + ?, last_mystery_box = ? WHERE id = ?')
      .run(amount, `${currentWeekStart} 00:00:00`, req.user.id);
    recordPieceTransaction(req.user.id, amount, 'mystery_box', `Coffre mystère · ${rarity}`);
    return { success: true, amount, rarity };
  })();
  if (!claim.success) {
    return res.status(429).json({
      ...claim,
      error: 'already_claimed_this_week',
      message: 'Tu as déjà ouvert ton coffre cette semaine. Reviens lundi !',
    });
  }
  const updated = db.prepare('SELECT pieces FROM users WHERE id = ?').get(req.user.id);
  res.json({ ...claim, pieces: updated.pieces });
});

app.get('/api/pieces/history', (req, res) => {
  const transactions = db.prepare(`SELECT amount, type, description, created_at AS createdAt
    FROM piece_transactions WHERE user_id = ? ORDER BY id DESC LIMIT 100`).all(req.user.id);
  res.json({ transactions });
});

const rewardAmounts = {
  complete_profile: 100,
  first_character_created: 150,
  first_message_sent: 50,
  five_characters_created: 300,
  first_share: 100,
  first_referral: 200,
  seven_day_streak: 200,
  thirty_day_streak: 500,
};

function rewardEligibility(user, action) {
  const characters = db.prepare('SELECT COUNT(*) AS count FROM characters WHERE user_id = ? AND is_system = 0')
    .get(user.id).count;
  const messages = db.prepare("SELECT COUNT(*) AS count FROM messages WHERE user_id = ? AND role = 'user'")
    .get(user.id).count;
  const shares = db.prepare('SELECT COUNT(*) AS count FROM shares WHERE user_id = ?').get(user.id).count;
  const referrals = db.prepare('SELECT COUNT(*) AS count FROM referrals WHERE referrer_id = ?').get(user.id).count;
  return ({
    complete_profile: Boolean(user.display_name.trim() && user.bio.trim()),
    first_character_created: characters >= 1,
    first_message_sent: messages >= 1,
    five_characters_created: characters >= 5,
    first_share: shares >= 1,
    first_referral: referrals >= 1,
    seven_day_streak: user.streak_days >= 7,
    thirty_day_streak: user.streak_days >= 30,
  })[action] ?? false;
}

app.get('/api/rewards', (req, res) => {
  const user = db.prepare('SELECT * FROM users WHERE id = ?').get(req.user.id);
  const claimedRewards = db.prepare('SELECT action, claimed_at FROM user_rewards WHERE user_id = ?').all(req.user.id);
  const claimedMap = new Map(claimedRewards.map((row) => [row.action, row.claimed_at]));
  
  const rewards = Object.entries(rewardAmounts).map(([action, amount]) => {
    const isClaimed = claimedMap.has(action);
    const conditionMet = rewardEligibility(user, action);
    let status;
    if (isClaimed) {
      status = 'claimed';
    } else if (conditionMet) {
      status = 'available';
    } else {
      status = 'locked';
    }
    return {
      action,
      reward: amount,
      status,
      claimed_at: isClaimed ? claimedMap.get(action) : null,
      condition_met: conditionMet,
    };
  });
  res.json({ rewards });
});

app.post('/api/rewards/action', (req, res) => {
  const action = typeof req.body?.action === 'string' ? req.body.action : '';
  const amount = rewardAmounts[action];
  const user = db.prepare('SELECT * FROM users WHERE id = ?').get(req.user.id);
  
  if (!amount) {
    return res.status(400).json({ success: false, error: 'invalid_action', message: 'Récompense invalide.' });
  }
  
  const alreadyClaimed = db.prepare('SELECT id FROM user_rewards WHERE user_id = ? AND action = ?').get(req.user.id, action);
  if (alreadyClaimed) {
    return res.status(409).json({ success: false, error: 'already_claimed', message: 'Tu as déjà réclamé cette récompense.' });
  }
  
  const conditionMet = rewardEligibility(user, action);
  if (!conditionMet) {
    return res.status(400).json({ success: false, error: 'condition_not_met', message: 'La condition pour cette récompense n\'est pas remplie.' });
  }
  
  try {
    const claim = db.transaction(() => {
      db.prepare('INSERT INTO user_rewards (user_id, action) VALUES (?, ?)').run(req.user.id, action);
      db.prepare('UPDATE users SET pieces = pieces + ? WHERE id = ?').run(amount, req.user.id);
      recordPieceTransaction(req.user.id, amount, 'reward', action);
    });
    claim();
    const updated = db.prepare('SELECT pieces FROM users WHERE id = ?').get(req.user.id);
    res.json({ success: true, pieces: updated.pieces });
  } catch (error) {
    if (error.code === 'SQLITE_CONSTRAINT_UNIQUE') {
      return res.status(409).json({ success: false, error: 'already_claimed', message: 'Tu as déjà réclamé cette récompense.' });
    }
    throw error;
  }
});

app.get('/api/quests', (req, res) => {
  const currentWeek = ensureUserQuests(req.user.id);
  const quests = db.prepare(`SELECT q.id, q.title, q.description, q.target, q.reward, q.type,
      uq.progress, uq.claimed
    FROM weekly_quests q JOIN user_quests uq ON uq.quest_id = q.id
    WHERE uq.user_id = ? AND uq.week_start = ? ORDER BY q.id`).all(req.user.id, currentWeek);
  res.json({ weekStart: currentWeek, quests });
});

app.post('/api/quests/:id/claim', (req, res) => {
  const questId = Number(req.params.id);
  const currentWeek = ensureUserQuests(req.user.id);
  const claim = db.transaction(() => {
    const quest = db.prepare(`SELECT q.*, uq.id AS user_quest_id, uq.progress, uq.claimed FROM weekly_quests q
      JOIN user_quests uq ON uq.quest_id = q.id WHERE q.id = ? AND uq.user_id = ? AND uq.week_start = ?`)
      .get(questId, req.user.id, currentWeek);
    if (!quest) return { status: 404, error: 'quest_not_found', message: 'Quête introuvable.' };
    if (quest.claimed) return { status: 409, error: 'already_claimed', message: 'Tu as déjà réclamé cette quête cette semaine.' };
    if (quest.progress < quest.target) return { status: 400, error: 'not_completed', message: 'La quête n\'est pas encore terminée.' };

    const update = db.prepare('UPDATE user_quests SET claimed = 1 WHERE id = ? AND claimed = 0 AND progress >= ?')
      .run(quest.user_quest_id, quest.target);
    if (!update.changes) return { status: 409, error: 'already_claimed', message: 'Tu as déjà réclamé cette quête cette semaine.' };
    db.prepare('UPDATE users SET pieces = pieces + ? WHERE id = ?').run(quest.reward, req.user.id);
    recordPieceTransaction(req.user.id, quest.reward, 'quest', quest.title);
    return { status: 200, reward: quest.reward };
  });
  const result = claim();
  if (result.status !== 200) {
    const { status, ...error } = result;
    return res.status(status).json({ success: false, ...error });
  }
  const updated = db.prepare('SELECT pieces FROM users WHERE id = ?').get(req.user.id);
  res.json({ success: true, reward: result.reward, pieces: updated.pieces });
});

app.get('/api/referral/code', (req, res) => res.json({ code: ensureReferralCode(req.user.id) }));

app.get('/api/referral/stats', (req, res) => {
  const stats = db.prepare('SELECT COUNT(*) AS count FROM referrals WHERE referrer_id = ?').get(req.user.id);
  res.json({ count: stats.count, total_earned: stats.count * 200 });
});

app.post('/api/referral/apply', (req, res) => {
  const code = typeof req.body?.code === 'string' ? req.body.code.trim().toUpperCase() : '';
  const referrer = db.prepare('SELECT id FROM users WHERE referral_code = ?').get(code);
  if (!referrer || referrer.id === req.user.id) return res.status(400).json({ error: 'Code de parrainage invalide.' });
  try {
    const now = new Date();
    const monthStart = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), 1)).toISOString().slice(0, 19).replace('T', ' ');
    const nextMonthStart = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth() + 1, 1)).toISOString().slice(0, 19).replace('T', ' ');
    const result = db.transaction(() => {
      const existingReferral = db.prepare('SELECT id FROM referrals WHERE referee_id = ?').get(req.user.id);
      if (existingReferral) return { status: 409, error: 'Un code a déjà été utilisé sur ce compte.' };
      const monthlyReferrals = db.prepare(`SELECT COUNT(*) AS count FROM referrals
        WHERE referrer_id = ? AND created_at >= ? AND created_at < ?`)
        .get(referrer.id, monthStart, nextMonthStart).count;
      if (monthlyReferrals >= 10) {
        return { status: 429, error: 'Ce code a atteint sa limite de 10 filleuls ce mois-ci.' };
      }
      db.prepare('INSERT INTO referrals (referrer_id, referee_id) VALUES (?, ?)').run(referrer.id, req.user.id);
      for (const userId of [referrer.id, req.user.id]) {
        db.prepare('UPDATE users SET pieces = pieces + 200 WHERE id = ?').run(userId);
        recordPieceTransaction(userId, 200, 'referral', 'Nouveau parrainage');
      }
      return { status: 200 };
    })();
    if (result.status !== 200) return res.status(result.status).json({ error: result.error });
  } catch (error) {
    if (error.code === 'SQLITE_CONSTRAINT_UNIQUE') return res.status(409).json({ error: 'Un code a déjà été utilisé sur ce compte.' });
    throw error;
  }
  res.json({ success: true, pieces: db.prepare('SELECT pieces FROM users WHERE id = ?').get(req.user.id).pieces });
});

app.post('/api/settings/theme', (req, res) => {
  const themes = ['violet', 'rose', 'blue', 'gold', 'emerald', 'amethyst', 'ruby', 'obsidian', 'ivory'];
  const premiumThemes = new Set(themes.slice(3));
  const theme = req.body?.theme;
  if (!themes.includes(theme)) return res.status(400).json({ error: 'Thème invalide.' });
  if (premiumThemes.has(theme) && !['premium', 'premium_plus'].includes(req.user.subscription_tier)) {
    return res.status(403).json({ error: 'Ce thème est réservé aux abonnés.' });
  }
  db.prepare('UPDATE users SET theme = ? WHERE id = ?').run(theme, req.user.id);
  res.json({ theme });
});

app.post('/api/settings/nsfw', (req, res) => {
  if (typeof req.body?.enabled !== 'boolean') {
    return res.status(400).json({ error: 'Le champ enabled doit être un booléen.' });
  }
  // For Google Play publication, consider moving this setting to the website.
  db.prepare('UPDATE users SET nsfw_enabled = ? WHERE id = ?').run(Number(req.body.enabled), req.user.id);
  res.json({ nsfw_enabled: req.body.enabled });
});

app.get('/api/characters', (req, res) => {
  const characters = db.prepare(`
    SELECT c.id, c.user_id, c.name, c.system_prompt, c.greeting, c.avatar_url, c.is_nsfw, c.is_public,
      COALESCE(state.relationship_level, c.relationship_level) AS relationship_level,
      COALESCE(state.last_mood, c.last_mood, 'neutral') AS last_mood, c.is_system, c.scenario_goal
    FROM characters c LEFT JOIN character_state state ON state.character_id = c.id AND state.user_id = ?
    WHERE c.user_id = ? OR c.is_system = 1
    ORDER BY c.is_system DESC, c.created_at DESC, c.id DESC
  `).all(req.user.id, req.user.id);
  res.json({ characters });
});

app.get('/api/characters/:id/avatar-status', (req, res) => {
  const characterId = Number(req.params.id);
  if (!Number.isSafeInteger(characterId)) return res.status(400).json({ error: 'Identifiant invalide.' });
  const character = db.prepare('SELECT avatar_url FROM characters WHERE id = ? AND user_id = ? AND is_system = 0')
    .get(characterId, req.user.id);
  if (!character) return res.status(404).json({ error: 'Personnage introuvable.' });
  res.json({ avatarUrl: character.avatar_url, generating: pendingCharacterImages.has(characterId) });
});

// Public characters routes
app.get('/api/characters/public', (req, res) => {
  const sort = req.query.sort ?? 'trending';
  const page = Number(req.query.page ?? 0);
  const requestedLimit = Number(req.query.limit ?? 20);
  const limit = Math.min(requestedLimit, 50);
  const query = typeof req.query.q === 'string' ? req.query.q.trim() : '';
  const includeNsfw = req.query.nsfw === 'include';
  if (!['trending', 'new', 'top'].includes(sort) ||
      !Number.isSafeInteger(page) || page < 0 ||
      !Number.isSafeInteger(requestedLimit) || requestedLimit < 1 ||
      !Number.isSafeInteger(page * limit) ||
      (req.query.q !== undefined && typeof req.query.q !== 'string') || query.length > 100) {
    return res.status(400).json({ error: 'Paramètres de catalogue invalides.' });
  }
  if (req.query.nsfw !== undefined && req.query.nsfw !== 'include') {
    return res.status(400).json({ error: 'Valeur nsfw invalide.' });
  }
  if (includeNsfw && !req.user.nsfw_enabled) {
    return res.status(403).json({ error: 'Active le paramètre NSFW pour voir ces personnages.' });
  }

  const filters = ['c.user_id IS NOT NULL', 'c.is_public = 1', 'c.is_system = 0'];
  const parameters = [];
  if (!includeNsfw) filters.push('c.is_nsfw = 0');
  if (query) {
    filters.push('c.name LIKE ?');
    parameters.push(`%${query}%`);
  }
  const where = filters.join(' AND ');
  const orderBy = {
    trending: 'c.likes_count DESC, c.created_at DESC',
    new: 'c.created_at DESC',
    top: 'c.likes_count DESC',
  }[sort];
  const total = db.prepare(`SELECT COUNT(*) AS count FROM characters c WHERE ${where}`)
    .get(...parameters).count;
  const rows = db.prepare(`
    SELECT c.id, c.name, c.greeting, c.avatar_url AS avatarUrl, c.is_nsfw AS isNsfw,
      c.likes_count AS likesCount, c.user_id AS creatorId,
      COALESCE(NULLIF(p.display_name, ''), NULLIF(u.display_name, ''), u.username, 'Utilisateur') AS creatorName,
      CASE
        WHEN u.badge_type IS NOT NULL THEN u.badge_type
        WHEN u.role = 'admin' THEN 'official'
        WHEN u.role = 'moderator' THEN 'moderator'
        WHEN u.subscription_tier = 'premium_plus' THEN 'premium_plus'
        WHEN u.subscription_tier = 'premium' THEN 'premium'
        WHEN (SELECT COUNT(*) FROM characters owned
          WHERE owned.user_id = u.id AND owned.is_public = 1 AND owned.is_system = 0
            AND owned.likes_count >= 50) >= 3 THEN 'creator'
        ELSE NULL
      END AS creatorBadge,
      NULLIF(c.scenario_goal, '') AS scenarioGoal,
      NULLIF(substr(c.system_prompt, 1, 300), '') AS description,
      strftime('%Y-%m-%dT%H:%M:%SZ', c.created_at) AS createdAt,
      EXISTS (SELECT 1 FROM characters imported
        WHERE imported.user_id = ? AND imported.imported_from = c.id) AS isImported,
      EXISTS (SELECT 1 FROM character_likes liked
        WHERE liked.user_id = ? AND liked.character_id = c.id) AS isLikedByCurrentUser
    FROM characters c
    JOIN users u ON u.id = c.user_id
    LEFT JOIN profiles p ON p.user_id = u.id
    WHERE ${where}
    ORDER BY ${orderBy}
    LIMIT ? OFFSET ?
  `).all(req.user.id, req.user.id, ...parameters, limit, page * limit);
  const characters = rows.map((character) => ({
    ...character,
    isNsfw: Boolean(character.isNsfw),
    isImported: Boolean(character.isImported),
  }));
  res.json({ characters, total, page, hasMore: page * limit + characters.length < total });
});

app.get('/api/characters/:id/public', (req, res) => {
  const characterId = Number(req.params.id);
  if (!Number.isSafeInteger(characterId)) return res.status(400).json({ error: 'Identifiant invalide.' });
  const character = db.prepare(`
    SELECT c.id, c.name, c.greeting, c.avatar_url AS avatarUrl, c.is_nsfw AS isNsfw,
      c.likes_count AS likesCount, c.user_id AS creatorId,
      COALESCE(NULLIF(p.display_name, ''), NULLIF(u.display_name, ''), u.username, 'Utilisateur') AS creatorName,
      c.scenario_goal AS scenarioGoal,
      NULLIF(substr(c.system_prompt, 1, 300), '') AS description,
      strftime('%Y-%m-%dT%H:%M:%SZ', c.created_at) AS createdAt,
      u.badge_type AS badgeType, u.role, u.subscription_tier AS subscriptionTier,
      EXISTS (SELECT 1 FROM characters imported
        WHERE imported.user_id = ? AND imported.imported_from = c.id) AS isImported,
      EXISTS (SELECT 1 FROM character_likes liked
        WHERE liked.user_id = ? AND liked.character_id = c.id) AS isLikedByCurrentUser
    FROM characters c
    JOIN users u ON u.id = c.user_id
    LEFT JOIN profiles p ON p.user_id = u.id
    WHERE c.id = ? AND c.user_id IS NOT NULL AND c.is_system = 0 AND c.is_public = 1
      AND (c.is_nsfw = 0 OR ? = 1)
  `).get(req.user.id, req.user.id, characterId, Number(req.user.nsfw_enabled));
  if (!character) return res.status(404).json({ error: 'Personnage public introuvable.' });
  const { badgeType, role, subscriptionTier, ...result } = character;
  res.json({
    ...result,
    creatorBadge: attributeBadge({
      id: character.creatorId,
      badge_type: badgeType,
      role,
      subscription_tier: subscriptionTier,
    }),
    isNsfw: Boolean(character.isNsfw),
    isImported: Boolean(character.isImported),
    isLikedByCurrentUser: Boolean(character.isLikedByCurrentUser),
  });
});

app.post('/api/characters/:id/publish', (req, res) => {
  const characterId = Number(req.params.id);
  if (!Number.isSafeInteger(characterId)) return res.status(400).json({ error: 'Identifiant invalide.' });
  const result = db.prepare(`UPDATE characters SET is_public = 1
    WHERE id = ? AND user_id = ? AND is_system = 0`).run(characterId, req.user.id);
  if (!result.changes) return res.status(404).json({ error: 'Personnage introuvable.' });
  res.json({ success: true });
});

app.post('/api/characters/:id/unpublish', (req, res) => {
  const characterId = Number(req.params.id);
  if (!Number.isSafeInteger(characterId)) return res.status(400).json({ error: 'Identifiant invalide.' });
  const result = db.prepare(`UPDATE characters SET is_public = 0
    WHERE id = ? AND user_id = ? AND is_system = 0`).run(characterId, req.user.id);
  if (!result.changes) return res.status(404).json({ error: 'Personnage introuvable.' });
  res.json({ success: true });
});

app.post('/api/characters/:id/like', (req, res) => {
  const characterId = Number(req.params.id);
  if (!Number.isSafeInteger(characterId)) return res.status(400).json({ error: 'Identifiant invalide.' });
  const character = db.prepare(`SELECT id, user_id FROM characters
    WHERE id = ? AND user_id IS NOT NULL AND is_system = 0 AND is_public = 1
      AND (is_nsfw = 0 OR ? = 1)`).get(characterId, Number(req.user.nsfw_enabled));
  if (!character) return res.status(404).json({ error: 'Personnage public introuvable.' });

  const result = db.transaction(() => {
    const existing = db.prepare(
      'SELECT id FROM character_likes WHERE character_id = ? AND user_id = ?'
    ).get(characterId, req.user.id);
    if (existing) {
      db.prepare('DELETE FROM character_likes WHERE id = ?').run(existing.id);
      db.prepare('UPDATE characters SET likes_count = MAX(0, likes_count - 1) WHERE id = ?').run(characterId);
      return { liked: false };
    }

    db.prepare('INSERT INTO character_likes (character_id, user_id) VALUES (?, ?)')
      .run(characterId, req.user.id);
    db.prepare('UPDATE characters SET likes_count = likes_count + 1 WHERE id = ?').run(characterId);
    if (character.user_id !== req.user.id) {
      db.prepare(`INSERT INTO notifications (user_id, type, actor_id, target_type, target_id)
        VALUES (?, 'character_like', ?, 'character', ?)`).run(character.user_id, req.user.id, characterId);
    }
    return { liked: true };
  })();
  const likesCount = db.prepare('SELECT likes_count FROM characters WHERE id = ?').get(characterId).likes_count;
  res.json({ ...result, likesCount });
});

app.post('/api/characters/:id/import', (req, res) => {
  const characterId = Number(req.params.id);
  if (!Number.isSafeInteger(characterId)) return res.status(400).json({ error: 'Identifiant invalide.' });

  const result = db.transaction(() => {
    const original = db.prepare(`SELECT * FROM characters
      WHERE id = ? AND user_id IS NOT NULL AND is_system = 0 AND is_public = 1
        AND (is_nsfw = 0 OR ? = 1)`).get(characterId, Number(req.user.nsfw_enabled));
    if (!original) return { status: 404, error: 'Personnage introuvable.' };

    const existing = db.prepare('SELECT id FROM characters WHERE user_id = ? AND imported_from = ?')
      .get(req.user.id, characterId);
    if (existing) return { status: 200, alreadyImported: true, id: existing.id };

    const inserted = db.prepare(`
      INSERT INTO characters (
        user_id, imported_from, name, system_prompt, greeting, avatar_url, is_nsfw,
        is_system, is_public, scenario_goal, boundaries, appearance
      ) VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, ?, ?, ?)
    `).run(
      req.user.id,
      characterId,
      original.name,
      original.system_prompt,
      original.greeting,
      original.avatar_url,
      original.is_nsfw,
      original.scenario_goal,
      original.boundaries,
      original.appearance,
    );
    return { status: 201, id: Number(inserted.lastInsertRowid) };
  })();

  const { status, ...body } = result;
  res.status(status).json(body);
});

app.post('/api/characters', (req, res) => {
  const {
    name, systemPrompt, greeting = 'Salut !', isNsfw = false,
    scenarioGoal = '', seedMemory = [], boundaries = '', appearance = '', bannedWords = '', scenarioSteps = [],
  } = req.body ?? {};
  if (typeof name !== 'string' || !name.trim() || name.length > 80 ||
      typeof systemPrompt !== 'string' || !systemPrompt.trim() || systemPrompt.length > 6000 ||
      typeof greeting !== 'string' || greeting.length > 500 || typeof isNsfw !== 'boolean' ||
      typeof scenarioGoal !== 'string' || scenarioGoal.length > 2000 || typeof boundaries !== 'string' || boundaries.length > 2000 ||
      typeof appearance !== 'string' || appearance.length > 1000 || typeof bannedWords !== 'string' || bannedWords.length > 1000 ||
      !Array.isArray(seedMemory) || seedMemory.length > 15 ||
      seedMemory.some((fact) => typeof fact !== 'string' || fact.trim().length > 300) ||
      !(typeof scenarioSteps === 'string' || Array.isArray(scenarioSteps))) {
    return res.status(400).json({ error: 'Informations du personnage invalides.' });
  }

  const creationCost = req.user.role === 'admin' ? 0 : 200;
  if (req.user.pieces < creationCost) {
    return res.status(402).json({ error: '200 Pièces sont nécessaires pour créer un personnage.', needPayment: true });
  }
  const create = db.transaction(() => {
    const result = db.prepare(`
      INSERT INTO characters (user_id, name, system_prompt, greeting, is_nsfw, scenario_goal,
        boundaries, appearance, banned_words, scenario_steps)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `).run(req.user.id, name.trim(), systemPrompt.trim(), greeting.trim() || 'Salut !', Number(isNsfw),
      scenarioGoal.trim(), boundaries.trim(), appearance.trim(), bannedWords.trim(),
      Array.isArray(scenarioSteps) ? JSON.stringify(scenarioSteps.slice(0, 20)) : scenarioSteps);
    const characterId = Number(result.lastInsertRowid);
    const addFact = db.prepare('INSERT INTO seed_memory (character_id, user_id, fact) VALUES (?, ?, ?)');
    const addSeedMemory = db.prepare('INSERT INTO character_seed_memory (character_id, fact) VALUES (?, ?)');
    for (const fact of seedMemory) {
      if (fact.trim()) {
        addFact.run(characterId, req.user.id, fact.trim());
        addSeedMemory.run(characterId, fact.trim());
      }
    }
    if (creationCost > 0) {
      db.prepare('UPDATE users SET pieces = pieces - ? WHERE id = ? AND pieces >= ?')
        .run(creationCost, req.user.id, creationCost);
      recordPieceTransaction(req.user.id, -creationCost, 'character_creation', `Création de ${name.trim()}`);
    }
    updateQuestProgress(req.user.id, 'characters', 1);
    return characterId;
  });
  const id = create();
  res.status(201).json({ id });
});

app.post('/api/characters/:id/avatar', (req, res) => {
  const characterId = Number(req.params.id);
  const character = Number.isSafeInteger(characterId)
    ? db.prepare('SELECT id FROM characters WHERE id = ? AND user_id = ? AND is_system = 0').get(characterId, req.user.id)
    : null;
  if (!character) return res.status(404).json({ error: 'Personnage introuvable.' });
  avatarUpload.single('avatar')(req, res, (error) => {
    if (error || !req.file) return res.status(400).json({ error: 'Image invalide (JPEG, PNG ou WebP, 5 Mo max).' });
    const extension = ({ 'image/jpeg': '.jpg', 'image/png': '.png', 'image/webp': '.webp' })[req.file.mimetype];
    const filename = `${req.file.filename}${extension}`;
    try {
      renameSync(req.file.path, resolve(uploadDirectory, filename));
    } catch {
      return res.status(500).json({ error: 'Impossible d’enregistrer l’image.' });
    }
    const avatarUrl = `${process.env.PUBLIC_BASE_URL ?? 'http://10.0.2.2:3000'}/uploads/${filename}`;
    db.prepare('UPDATE characters SET avatar_url = ? WHERE id = ? AND user_id = ?')
      .run(avatarUrl, characterId, req.user.id);
    return res.json({ avatar_url: avatarUrl });
  });
});

app.delete('/api/characters/:id', (req, res) => {
  const characterId = Number(req.params.id);
  if (!Number.isSafeInteger(characterId)) return res.status(400).json({ error: 'Identifiant invalide.' });
  const character = ownedCharacter(characterId, req.user.id);
  if (!character || character.is_system) return res.status(404).json({ error: 'Personnage introuvable.' });

  const remove = db.transaction(() => {
    db.prepare('DELETE FROM messages WHERE user_id = ? AND character_id = ?').run(req.user.id, characterId);
    db.prepare('DELETE FROM characters WHERE id = ? AND user_id = ?').run(characterId, req.user.id);
  });
  remove();
  res.status(204).end();
});

app.get('/api/characters/:id/memory', (req, res) => {
  const characterId = Number(req.params.id);
  if (!ownedCharacter(characterId, req.user.id)) return res.status(404).json({ error: 'Personnage introuvable.' });
  const seedMemory = db.prepare('SELECT fact FROM seed_memory WHERE character_id = ? AND user_id = ? ORDER BY id')
    .all(characterId, req.user.id).map((row) => row.fact);
  const learnedMemory = db.prepare('SELECT fact FROM learned_memory WHERE character_id = ? AND user_id = ? ORDER BY id')
    .all(characterId, req.user.id).map((row) => row.fact);
  const pinned = db.prepare('SELECT message_text AS text, created_at AS createdAt FROM pinned_messages WHERE character_id = ? AND user_id = ? ORDER BY id DESC')
    .all(characterId, req.user.id);
  const structured = db.prepare('SELECT category, key, value, confidence FROM structured_memory WHERE character_id = ? AND user_id = ? ORDER BY category, key')
    .all(characterId, req.user.id);
  res.json({ seedMemory, learnedMemory, pinned, structured });
});

app.delete('/api/characters/:id/memory/:type/:index', (req, res) => {
  const characterId = Number(req.params.id);
  const factIndex = Number(req.params.index);
  const table = req.params.type === 'seed' ? 'seed_memory'
    : req.params.type === 'learned' ? 'learned_memory' : null;
  if (!ownedCharacter(characterId, req.user.id)) return res.status(404).json({ error: 'Personnage introuvable.' });
  if (!table || !Number.isSafeInteger(factIndex) || factIndex < 0) {
    return res.status(400).json({ error: 'Type ou index invalide.' });
  }
  const row = db.prepare(`SELECT id FROM ${table} WHERE character_id = ? AND user_id = ? ORDER BY id LIMIT 1 OFFSET ?`)
    .get(characterId, req.user.id, factIndex);
  if (!row) return res.status(404).json({ error: 'Fait introuvable.' });
  db.prepare(`DELETE FROM ${table} WHERE id = ?`).run(row.id);
  res.status(204).end();
});

app.post('/api/characters/:id/pin', (req, res) => {
  const characterId = Number(req.params.id);
  const text = typeof req.body?.messageText === 'string' ? req.body.messageText.trim() : '';
  if (!ownedCharacter(characterId, req.user.id)) return res.status(404).json({ error: 'Personnage introuvable.' });
  if (!text || text.length > 4000) return res.status(400).json({ error: 'Message à épingler invalide.' });
  const count = db.prepare('SELECT COUNT(*) AS count FROM pinned_messages WHERE character_id = ? AND user_id = ?')
    .get(characterId, req.user.id).count;
  if (count >= 10) return res.status(409).json({ error: 'Dix messages sont déjà épinglés.' });
  const result = db.prepare('INSERT INTO pinned_messages (character_id, user_id, message_text) VALUES (?, ?, ?)')
    .run(characterId, req.user.id, text);
  res.status(201).json({ id: Number(result.lastInsertRowid) });
});

app.delete('/api/characters/:id/pin/:index', (req, res) => {
  const characterId = Number(req.params.id);
  const index = Number(req.params.index);
  if (!ownedCharacter(characterId, req.user.id)) return res.status(404).json({ error: 'Personnage introuvable.' });
  const row = db.prepare('SELECT id FROM pinned_messages WHERE character_id = ? AND user_id = ? ORDER BY id DESC LIMIT 1 OFFSET ?')
    .get(characterId, req.user.id, index);
  if (!row) return res.status(404).json({ error: 'Épingle introuvable.' });
  db.prepare('DELETE FROM pinned_messages WHERE id = ?').run(row.id);
  res.status(204).end();
});

app.get('/api/characters/:id/events', (req, res) => {
  const id = Number(req.params.id);
  if (!ownedCharacter(id, req.user.id)) return res.status(404).json({ error: 'Personnage introuvable.' });
  res.json({ events: db.prepare('SELECT id, event_date AS eventDate, label, recurring FROM character_events WHERE character_id = ? AND user_id = ? ORDER BY event_date')
    .all(id, req.user.id) });
});

app.post('/api/characters/:id/events', (req, res) => {
  const id = Number(req.params.id);
  const eventDate = typeof req.body?.eventDate === 'string' ? req.body.eventDate : '';
  const label = typeof req.body?.label === 'string' ? req.body.label.trim() : '';
  if (!ownedCharacter(id, req.user.id)) return res.status(404).json({ error: 'Personnage introuvable.' });
  if (!/^\d{4}-\d{2}-\d{2}$/.test(eventDate) || !label || label.length > 120) return res.status(400).json({ error: 'Événement invalide.' });
  const result = db.prepare('INSERT INTO character_events (character_id, user_id, event_date, label, recurring) VALUES (?, ?, ?, ?, ?)')
    .run(id, req.user.id, eventDate, label, Number(req.body?.recurring === true));
  res.status(201).json({ id: Number(result.lastInsertRowid) });
});

app.get('/api/characters/:id/notes', (req, res) => {
  const id = Number(req.params.id);
  if (!ownedCharacter(id, req.user.id)) return res.status(404).json({ error: 'Personnage introuvable.' });
  res.json({ notes: db.prepare('SELECT id, content, created_at AS createdAt FROM character_notes WHERE character_id = ? AND user_id = ? ORDER BY id DESC')
    .all(id, req.user.id) });
});

app.post('/api/characters/:id/notes', (req, res) => {
  const id = Number(req.params.id);
  const content = typeof req.body?.content === 'string' ? req.body.content.trim() : '';
  if (!ownedCharacter(id, req.user.id)) return res.status(404).json({ error: 'Personnage introuvable.' });
  if (!content || content.length > 4000) return res.status(400).json({ error: 'Note invalide.' });
  const result = db.prepare('INSERT INTO character_notes (character_id, user_id, content) VALUES (?, ?, ?)')
    .run(id, req.user.id, content);
  res.status(201).json({ id: Number(result.lastInsertRowid) });
});

app.get('/api/characters/:id/gallery', (req, res) => {
  const id = Number(req.params.id);
  if (!ownedCharacter(id, req.user.id)) return res.status(404).json({ error: 'Personnage introuvable.' });
  res.json({ images: db.prepare('SELECT id, image_url AS imageUrl, caption, created_at AS createdAt FROM character_gallery WHERE character_id = ? AND user_id = ? ORDER BY id DESC')
    .all(id, req.user.id) });
});

app.get('/api/characters/:id/journal', (req, res) => {
  const id = Number(req.params.id);
  if (!ownedCharacter(id, req.user.id)) return res.status(404).json({ error: 'Personnage introuvable.' });
  res.json({ events: db.prepare('SELECT id, event_type AS eventType, description, created_at AS createdAt FROM character_journal WHERE character_id = ? AND user_id = ? ORDER BY id DESC LIMIT 200')
    .all(id, req.user.id) });
});

app.get('/api/characters/:id/thought', async (req, res) => {
  const characterId = Number(req.params.id);
  const character = ownedCharacter(characterId, req.user.id);
  if (!character) return res.status(404).json({ error: 'Personnage introuvable.' });
  const state = db.prepare('SELECT thought_text, thought_generated_at FROM character_state WHERE character_id = ? AND user_id = ?')
    .get(characterId, req.user.id);
  if (state?.thought_text && Date.now() - Date.parse(`${state.thought_generated_at}Z`) < 24 * 60 * 60 * 1000) {
    return res.json({ thought: state.thought_text, cached: true });
  }
  try {
    const thought = await callMistral([
      { role: 'system', content: 'Écris une pensée intérieure douce et courte (une phrase) pour ce personnage, sans mentionner une IA.' },
      { role: 'user', content: buildSystemPrompt(character, req.user.id) },
    ], Boolean(req.user.nsfw_enabled));
    db.prepare(`INSERT INTO character_state (character_id, user_id, relationship_level, last_mood, thought_text, thought_generated_at)
      VALUES (?, ?, 0, 'neutral', ?, CURRENT_TIMESTAMP) ON CONFLICT(character_id, user_id) DO UPDATE SET
      thought_text = excluded.thought_text, thought_generated_at = CURRENT_TIMESTAMP`)
      .run(characterId, req.user.id, thought);
    res.json({ thought, cached: false });
  } catch {
    res.status(503).json({ error: 'Pensée du jour indisponible.' });
  }
});

app.get('/api/characters/:id/messages', (req, res) => {
  const characterId = Number(req.params.id);
  if (!ownedCharacter(characterId, req.user.id)) return res.status(404).json({ error: 'Personnage introuvable.' });
  const page = Math.max(0, Number(req.query.page) || 0);
  const limit = Math.min(100, Math.max(1, Number(req.query.limit) || 50));
  const messages = db.prepare(`SELECT id, role, content, is_favorite AS isFavorite, created_at AS createdAt
    FROM messages WHERE user_id = ? AND character_id = ? ORDER BY id DESC LIMIT ? OFFSET ?`)
    .all(req.user.id, characterId, limit, page * limit).reverse();
  res.json({ messages, page, limit, hasMore: messages.length === limit });
});

app.put('/api/messages/:id', (req, res) => {
  const id = Number(req.params.id);
  const content = typeof req.body?.content === 'string' ? req.body.content.trim() : '';
  if (!content || content.length > 8000) return res.status(400).json({ error: 'Message invalide.' });
  const update = db.prepare('UPDATE messages SET content = ? WHERE id = ? AND user_id = ?').run(content, id, req.user.id);
  if (!update.changes) return res.status(404).json({ error: 'Message introuvable.' });
  res.json({ id, content });
});

app.delete('/api/messages/:id', (req, res) => {
  const id = Number(req.params.id);
  const result = db.prepare('DELETE FROM messages WHERE id = ? AND user_id = ?').run(id, req.user.id);
  if (!result.changes) return res.status(404).json({ error: 'Message introuvable.' });
  res.status(204).end();
});

app.post('/api/messages/:id/favorite', (req, res) => {
  const id = Number(req.params.id);
  const message = db.prepare('SELECT is_favorite FROM messages WHERE id = ? AND user_id = ?').get(id, req.user.id);
  if (!message) return res.status(404).json({ error: 'Message introuvable.' });
  const favorite = req.body?.favorite == null ? !message.is_favorite : req.body.favorite === true;
  db.prepare('UPDATE messages SET is_favorite = ? WHERE id = ? AND user_id = ?').run(Number(favorite), id, req.user.id);
  res.json({ favorite });
});

app.post('/api/messages/:id/regenerate', async (req, res) => {
  const id = Number(req.params.id);
  const assistantMessage = db.prepare("SELECT * FROM messages WHERE id = ? AND user_id = ? AND role = 'assistant'")
    .get(id, req.user.id);
  if (!assistantMessage) return res.status(404).json({ error: 'Réponse introuvable.' });
  const userMessage = db.prepare(`SELECT * FROM messages WHERE user_id = ? AND character_id IS ? AND role = 'user' AND id < ?
    ORDER BY id DESC LIMIT 1`).get(req.user.id, assistantMessage.character_id, id);
  if (!userMessage) return res.status(409).json({ error: 'Message utilisateur associé introuvable.' });
  const cost = 1;
  if (req.user.pieces < cost) return res.status(402).json({ error: 'Vous n’avez plus de Pièces.', needPayment: true });
  const character = assistantMessage.character_id == null ? null : ownedCharacter(assistantMessage.character_id, req.user.id);
  const prompt = buildSystemPrompt(character, req.user.id);
  const history = db.prepare(`SELECT role, content FROM messages WHERE user_id = ? AND character_id IS ? AND id < ? ORDER BY id DESC LIMIT 20`)
    .all(req.user.id, assistantMessage.character_id, userMessage.id).reverse();
  try {
    const reply = await callMistral([
      { role: 'system', content: prompt }, ...history,
      { role: 'user', content: userMessage.content },
    ], Boolean(req.user.nsfw_enabled));
    const regenerate = db.transaction(() => {
      const debit = db.prepare('UPDATE users SET pieces = pieces - 1 WHERE id = ? AND pieces > 0').run(req.user.id);
      if (!debit.changes) throw new Error('INSUFFICIENT_PIECES');
      db.prepare('UPDATE messages SET content = ? WHERE id = ?').run(reply, id);
      recordPieceTransaction(req.user.id, -1, 'regenerate', 'Régénération de réponse');
    });
    regenerate();
    res.json({ id, reply, pieces: db.prepare('SELECT pieces FROM users WHERE id = ?').get(req.user.id).pieces });
  } catch (error) {
    res.status(error.message === 'INSUFFICIENT_PIECES' ? 402 : error.message === 'BLOCKED_BY_GUARDRAIL' ? 403 : 502)
      .json({ error: error.message === 'BLOCKED_BY_GUARDRAIL' ? 'Message bloqué par les règles de modération.' : 'Régénération indisponible.' });
  }
});

app.post('/api/characters/:id/rewind', (req, res) => {
  const characterId = Number(req.params.id);
  const messageId = Number(req.body?.messageId);
  if (!ownedCharacter(characterId, req.user.id)) return res.status(404).json({ error: 'Personnage introuvable.' });
  const message = db.prepare('SELECT id FROM messages WHERE id = ? AND user_id = ? AND character_id = ?')
    .get(messageId, req.user.id, characterId);
  if (!message) return res.status(404).json({ error: 'Message introuvable.' });
  db.prepare('DELETE FROM messages WHERE user_id = ? AND character_id = ? AND id > ?')
    .run(req.user.id, characterId, messageId);
  res.json({ success: true });
});

app.get('/api/search', (req, res) => {
  const query = typeof req.query.q === 'string' ? req.query.q.trim() : '';
  if (!query || query.length > 200) return res.status(400).json({ error: 'Requête de recherche invalide.' });
  const characterId = req.query.characterId == null ? null : Number(req.query.characterId);
  const results = db.prepare(`SELECT id, character_id AS characterId, role, content, created_at AS createdAt
    FROM messages WHERE user_id = ? AND content LIKE ? AND (? IS NULL OR character_id = ?)
    ORDER BY id DESC LIMIT 100`).all(req.user.id, `%${query}%`, characterId, characterId);
  res.json({ results });
});

app.get('/api/characters/:id/export', (req, res) => {
  const id = Number(req.params.id);
  const character = ownedCharacter(id, req.user.id);
  if (!character) return res.status(404).json({ error: 'Personnage introuvable.' });
  const format = req.query.format ?? 'txt';
  if (!['txt', 'json', 'html', 'pdf'].includes(format)) return res.status(400).json({ error: 'Format invalide.' });
  if (format === 'pdf') return res.status(501).json({ error: 'L’export PDF nécessite un convertisseur PDF configuré.' });
  const messages = db.prepare('SELECT role, content, created_at AS createdAt FROM messages WHERE user_id = ? AND character_id = ? ORDER BY id')
    .all(req.user.id, id);
  const payload = { character: { name: character.name, greeting: character.greeting }, messages };
  const body = format === 'json' ? JSON.stringify(payload, null, 2)
    : format === 'html' ? `<!doctype html><meta charset="utf-8"><title>${escapeHtml(character.name)}</title><h1>${escapeHtml(character.name)}</h1>${messages.map((item) => `<p><b>${escapeHtml(item.role)}:</b> ${escapeHtml(item.content)}</p>`).join('')}`
      : messages.map((item) => `${item.role === 'user' ? 'Vous' : character.name} : ${item.content}`).join('\n\n');
  res.type(format === 'json' ? 'application/json' : format === 'html' ? 'text/html' : 'text/plain')
    .attachment(`${character.name}.${format}`).send(body);
});

app.get('/api/characters/:id/share', (req, res) => {
  const characterId = Number(req.params.id);
  const character = Number.isSafeInteger(characterId)
    ? db.prepare('SELECT id FROM characters WHERE id = ? AND user_id = ? AND is_system = 0').get(characterId, req.user.id)
    : null;
  if (!character) return res.status(404).json({ error: 'Personnage introuvable.' });
  const token = randomUUID().replaceAll('-', '');
  db.prepare('INSERT INTO shares (token, character_id, user_id) VALUES (?, ?, ?)')
    .run(token, characterId, req.user.id);
  res.json({ url: `https://chatile.app/c/${token}`, token });
});

app.get('/api/shared/:token', (req, res) => {
  const character = db.prepare(`
    SELECT c.name, c.system_prompt, c.greeting, c.avatar_url
    FROM shares s JOIN characters c ON c.id = s.character_id WHERE s.token = ?
  `).get(req.params.token);
  if (!character) return res.status(404).json({ error: 'Lien de partage introuvable.' });
  res.json(character);
});



app.get('/api/stats', (req, res) => {
  const messagesByCharacter = db.prepare(`
    SELECT COALESCE(c.name, 'Assistant IA') AS characterName, COUNT(*) AS messages
    FROM messages m LEFT JOIN characters c ON c.id = m.character_id
    WHERE m.user_id = ? AND m.role = 'user' GROUP BY m.character_id ORDER BY messages DESC
  `).all(req.user.id);
  res.json({
    messagesByCharacter,
    preferredCharacter: messagesByCharacter[0]?.characterName ?? null,
    totalMessages: messagesByCharacter.reduce((sum, row) => sum + row.messages, 0),
  });
});

app.post('/api/chat/cowrite', async (req, res) => {
  const characterId = Number(req.body?.characterId);
  const lastParagraph = typeof req.body?.lastParagraph === 'string' ? req.body.lastParagraph.trim() : '';
  const character = ownedCharacter(characterId, req.user.id);
  if (!character) return res.status(404).json({ error: 'Personnage introuvable.' });
  if (!lastParagraph || lastParagraph.length > 5000) return res.status(400).json({ error: 'Paragraphe invalide.' });
  if (req.user.pieces < 1) return res.status(402).json({ error: 'Vous n’avez plus de Pièces.', needPayment: true });
  try {
    const reply = await callMistral([
      { role: 'system', content: `${buildSystemPrompt(character, req.user.id)}\nÉcris UN paragraphe (3-5 phrases) qui suit naturellement le dernier paragraphe. Ne conclus pas.` },
      { role: 'user', content: lastParagraph },
    ], Boolean(req.user.nsfw_enabled));
    const save = db.transaction(() => {
      const debit = db.prepare('UPDATE users SET pieces = pieces - 1 WHERE id = ? AND pieces > 0').run(req.user.id);
      if (!debit.changes) throw new Error('INSUFFICIENT_PIECES');
      recordPieceTransaction(req.user.id, -1, 'cowrite', `Co-écriture avec ${character.name}`);
    });
    save();
    res.json({ reply, pieces: db.prepare('SELECT pieces FROM users WHERE id = ?').get(req.user.id).pieces });
  } catch (error) {
    res.status(error.message === 'INSUFFICIENT_PIECES' ? 402 : error.message === 'BLOCKED_BY_GUARDRAIL' ? 403 : 502)
      .json({ error: error.message === 'BLOCKED_BY_GUARDRAIL' ? 'Message bloqué par les règles de modération.' : 'Co-écriture indisponible.' });
  }
});

app.post('/api/mood', (req, res) => {
  const mood = typeof req.body?.mood === 'string' ? req.body.mood.trim() : '';
  const note = typeof req.body?.note === 'string' ? req.body.note.trim() : '';
  const characterId = req.body?.characterId == null ? null : Number(req.body.characterId);
  if (!['heureux', 'calme', 'stressé', 'triste', 'motivé', 'fatigué', 'anxieux'].includes(mood) || note.length > 1000 ||
      (characterId != null && !ownedCharacter(characterId, req.user.id))) return res.status(400).json({ error: 'Humeur invalide.' });
  db.prepare('INSERT INTO mood_journal (user_id, character_id, mood, note) VALUES (?, ?, ?, ?)')
    .run(req.user.id, characterId, mood, note);
  res.status(201).json({ mood, note });
});

app.get('/api/mood/today', (req, res) => {
  const mood = db.prepare(`SELECT mood, note, created_at AS createdAt FROM mood_journal
    WHERE user_id = ? AND created_at >= date('now') ORDER BY id DESC LIMIT 1`).get(req.user.id);
  res.json({ mood: mood ?? null });
});

app.post('/api/challenge/generate', async (req, res) => {
  const characterId = Number(req.body?.characterId);
  const character = ownedCharacter(characterId, req.user.id);
  if (!character) return res.status(404).json({ error: 'Personnage introuvable.' });
  try {
    const description = await callMistral([
      { role: 'system', content: 'Propose un défi quotidien simple, bienveillant et réalisable en une phrase. Réponds uniquement par le défi.' },
      { role: 'user', content: buildSystemPrompt(character, req.user.id) },
    ], Boolean(req.user.nsfw_enabled));
    const result = db.prepare('INSERT INTO challenges (user_id, character_id, description) VALUES (?, ?, ?)')
      .run(req.user.id, characterId, description.slice(0, 500));
    res.status(201).json({ id: Number(result.lastInsertRowid), description });
  } catch {
    res.status(503).json({ error: 'Génération du défi indisponible.' });
  }
});

app.post('/api/challenge/:id/complete', (req, res) => {
  const id = Number(req.params.id);
  const challenge = db.prepare('SELECT * FROM challenges WHERE id = ? AND user_id = ?').get(id, req.user.id);
  if (!challenge) return res.status(404).json({ error: 'Défi introuvable.' });
  if (challenge.completed) return res.status(409).json({ error: 'Défi déjà complété.' });
  const reward = req.user.subscription_tier === 'free' ? 5 : 10;
  const complete = db.transaction(() => {
    db.prepare('UPDATE challenges SET completed = 1 WHERE id = ?').run(id);
    db.prepare('UPDATE users SET pieces = pieces + ? WHERE id = ?').run(reward, req.user.id);
    recordPieceTransaction(req.user.id, reward, 'challenge', 'Défi complété');
  });
  complete();
  res.json({ success: true, reward, pieces: db.prepare('SELECT pieces FROM users WHERE id = ?').get(req.user.id).pieces });
});

app.get('/api/retrospective', (req, res) => {
  const rows = db.prepare(`SELECT COALESCE(c.name, 'Assistant IA') AS characterName, COUNT(*) AS messages
    FROM messages m LEFT JOIN characters c ON c.id = m.character_id
    WHERE m.user_id = ? AND m.role = 'user' AND m.created_at >= datetime('now', '-7 days')
    GROUP BY m.character_id ORDER BY messages DESC`).all(req.user.id);
  res.json({ weekMessages: rows.reduce((total, row) => total + row.messages, 0), byCharacter: rows });
});

app.get('/api/widget-data', (req, res) => {
  const today = utcDate();
  const character = db.prepare('SELECT id, name FROM characters WHERE is_system = 1 ORDER BY id LIMIT 1 OFFSET ?')
    .get(Math.floor(Date.now() / 86_400_000) % systemCharacters.length);
  res.json({ pieces: req.user.pieces, dailyCanClaim: dailyClaimState(req.user).canClaim, characterOfTheDay: character });
});

app.get('/api/character-of-the-day', (req, res) => {
  const character = db.prepare('SELECT id, name, avatar_url AS avatarUrl FROM characters WHERE is_system = 1 ORDER BY id LIMIT 1 OFFSET ?')
    .get(Math.floor(Date.now() / 86_400_000) % systemCharacters.length);
  res.json({ character });
});

app.post('/api/tutorial/step', (req, res) => {
  const step = Number(req.body?.step);
  if (!Number.isInteger(step) || step < 1 || step > 5) return res.status(400).json({ error: 'Étape invalide.' });
  const action = `tutorial_step_${step}`;
  try {
    const reward = db.transaction(() => {
      db.prepare('INSERT INTO user_rewards (user_id, action) VALUES (?, ?)').run(req.user.id, action);
      db.prepare('UPDATE users SET pieces = pieces + 10, tutorial_completed = CASE WHEN ? = 5 THEN 1 ELSE tutorial_completed END WHERE id = ?')
        .run(step, req.user.id);
      recordPieceTransaction(req.user.id, 10, 'tutorial', `Tutoriel · étape ${step}`);
    });
    reward();
  } catch (error) {
    if (error.code === 'SQLITE_CONSTRAINT_UNIQUE') return res.status(409).json({ error: 'Étape déjà terminée.' });
    throw error;
  }
  res.json({ success: true, pieces: db.prepare('SELECT pieces FROM users WHERE id = ?').get(req.user.id).pieces });
});

app.post('/api/tutorial/skip', (req, res) => {
  db.prepare('UPDATE users SET tutorial_completed = 1 WHERE id = ?').run(req.user.id);
  res.json({ success: true });
});

app.post('/api/chat', async (req, res) => {
  const message = typeof req.body?.message === 'string' ? req.body.message.trim() : '';
  const characterId = req.body?.characterId == null ? null : Number(req.body.characterId);
    const usePremiumModel = req.body?.usePremiumModel === true;
    const mode = typeof req.body?.mode === 'string' ? req.body.mode : 'normal';
  if (!message || message.length > 8000 || (characterId !== null && !Number.isSafeInteger(characterId)) ||
      (req.body?.usePremiumModel != null && typeof req.body.usePremiumModel !== 'boolean') ||
      !['normal', 'focus', 'story', 'hard'].includes(mode)) {
    return res.status(400).json({ error: 'Message ou personnage invalide.' });
  }
  const subscriptionActive = ['premium', 'premium_plus'].includes(req.user.subscription_tier) &&
    req.user.subscription_expires_at && Date.parse(req.user.subscription_expires_at) > Date.now();
  const cost = usePremiumModel ? (subscriptionActive ? 1 : 3) : 1;
  if (req.user.pieces < cost) return res.status(402).json({ error: 'Vous n’avez plus assez de Pièces.', needPayment: true });

  const character = characterId === null ? null : ownedCharacter(characterId, req.user.id);
  if (characterId !== null && !character) return res.status(404).json({ error: 'Personnage introuvable.' });
  if (character?.is_system && req.user.subscription_tier === 'free') {
    const todayMessages = db.prepare(`SELECT COUNT(*) AS count FROM messages WHERE user_id = ? AND character_id = ?
      AND role = 'user' AND created_at >= date('now')`).get(req.user.id, character.id).count;
    if (todayMessages >= 5) return res.status(429).json({ error: 'Limite gratuite de 5 messages quotidiens atteinte pour ce personnage.' });
  }

  const systemContent = buildSystemPrompt(character, req.user.id);
  const history = db.prepare(`
    SELECT role, content FROM messages
    WHERE user_id = ? AND character_id IS ?
    ORDER BY id DESC LIMIT 20
  `).all(req.user.id, characterId).reverse();
  const mistralMessages = [
    { role: 'system', content: systemContent },
    ...history.map(({ role, content }) => ({ role, content })),
    { role: 'user', content: message },
  ];
  const characterMessageCount = db.prepare(`SELECT COUNT(*) AS count FROM messages
    WHERE user_id = ? AND character_id IS ? AND role = 'user'`).get(req.user.id, characterId).count;
  const showInterstitial = !subscriptionActive && mode !== 'focus' && characterMessageCount >= 50 &&
    req.user.messages_since_last_ad >= 49;

  try {
    const reply = await callMistral(
      mistralMessages,
      Boolean(req.user.nsfw_enabled),
      usePremiumModel ? 'mistral-large-2603' : 'mistral-small-2603',
    );
    const saveAndCharge = db.transaction(() => {
      const debit = db.prepare('UPDATE users SET pieces = pieces - ? WHERE id = ? AND pieces >= ?')
        .run(cost, req.user.id, cost);
      if (debit.changes !== 1) throw new Error('INSUFFICIENT_PIECES');
      const insert = db.prepare(
        'INSERT INTO messages (user_id, character_id, role, content) VALUES (?, ?, ?, ?)',
      );
      const userMessage = insert.run(req.user.id, characterId, 'user', message);
      const assistantMessage = insert.run(req.user.id, characterId, 'assistant', reply);
      recordPieceTransaction(req.user.id, -cost, 'chat', usePremiumModel ? 'Message Mistral Large 3' : 'Message Mistral Small 4');
      db.prepare(`UPDATE users SET messages_since_last_ad = CASE WHEN ? THEN 0 ELSE messages_since_last_ad + 1 END
        WHERE id = ?`).run(Number(showInterstitial), req.user.id);
      db.prepare(`INSERT INTO stats (user_id, character_id, messages_count, last_interaction)
        VALUES (?, ?, 1, CURRENT_TIMESTAMP) ON CONFLICT(user_id, character_id) DO UPDATE SET
        messages_count = messages_count + 1, last_interaction = CURRENT_TIMESTAMP`)
        .run(req.user.id, characterId);
      updateQuestProgress(req.user.id, 'messages', 1);
      if (character) {
        db.prepare(`INSERT INTO character_state (character_id, user_id, relationship_level, last_mood)
          VALUES (?, ?, 1, 'neutral') ON CONFLICT(character_id, user_id) DO UPDATE SET
          relationship_level = MIN(10, relationship_level + 1)`)
          .run(character.id, req.user.id);
      }
      return { userMessageId: Number(userMessage.lastInsertRowid), assistantMessageId: Number(assistantMessage.lastInsertRowid) };
    });
    const savedMessages = saveAndCharge();

    if (character) void extractMemory(
      character.id,
      req.user.id,
      `Utilisateur : ${message}\nAssistant : ${reply}`,
      Boolean(req.user.nsfw_enabled),
    );
    const updated = db.prepare('SELECT pieces FROM users WHERE id = ?').get(req.user.id);
    const currentCharacter = character
      ? db.prepare(`SELECT COALESCE(state.relationship_level, character.relationship_level) AS relationship_level,
          COALESCE(state.last_mood, character.last_mood, 'neutral') AS last_mood
        FROM characters character LEFT JOIN character_state state
          ON state.character_id = character.id AND state.user_id = ? WHERE character.id = ?`)
        .get(req.user.id, character.id)
      : { relationship_level: 0, last_mood: 'neutral' };
    res.json({
      reply,
      pieces: updated.pieces,
      relationship_level: currentCharacter.relationship_level,
      last_mood: currentCharacter.last_mood ?? 'neutral',
      userMessageId: savedMessages.userMessageId,
      assistantMessageId: savedMessages.assistantMessageId,
      suggestions: ['Parle-moi davantage de ça', 'Et ensuite, qu’est-ce qui se passe ?', 'Comment tu te sens ?'],
      showInterstitial,
    });
  } catch (error) {
    if (error.message === 'BLOCKED_BY_GUARDRAIL') {
      return res.status(403).json({ error: 'Ce message a été bloqué par les règles de modération.' });
    }
    if (error.message === 'INSUFFICIENT_PIECES') {
      return res.status(402).json({ error: 'Vous n’avez plus de Pièces.', needPayment: true });
    }

    // Smart fallback reply so chat never fails with "Service IA indisponible"
    console.warn('Mistral AI unavailable, using smart local fallback response:', error.message);
    const reply = getFallbackReply(character, message);
    try {
      const saveAndCharge = db.transaction(() => {
        const debit = db.prepare('UPDATE users SET pieces = pieces - ? WHERE id = ? AND pieces >= ?')
          .run(cost, req.user.id, cost);
        if (debit.changes !== 1) throw new Error('INSUFFICIENT_PIECES');
        const insert = db.prepare(
          'INSERT INTO messages (user_id, character_id, role, content) VALUES (?, ?, ?, ?)',
        );
        const userMessage = insert.run(req.user.id, characterId, 'user', message);
        const assistantMessage = insert.run(req.user.id, characterId, 'assistant', reply);
        recordPieceTransaction(req.user.id, -cost, 'chat', 'Message de secours IA');
        return { userMessageId: Number(userMessage.lastInsertRowid), assistantMessageId: Number(assistantMessage.lastInsertRowid) };
      });
      const savedMessages = saveAndCharge();
      const updated = db.prepare('SELECT pieces FROM users WHERE id = ?').get(req.user.id);
      const currentCharacter = character
        ? db.prepare(`SELECT COALESCE(state.relationship_level, character.relationship_level) AS relationship_level,
            COALESCE(state.last_mood, character.last_mood, 'neutral') AS last_mood
          FROM characters character LEFT JOIN character_state state
            ON state.character_id = character.id AND state.user_id = ? WHERE character.id = ?`)
          .get(req.user.id, character.id)
        : { relationship_level: 0, last_mood: 'neutral' };
      return res.json({
        reply,
        pieces: updated.pieces,
        relationship_level: currentCharacter.relationship_level,
        last_mood: currentCharacter.last_mood ?? 'neutral',
        userMessageId: savedMessages.userMessageId,
        assistantMessageId: savedMessages.assistantMessageId,
        suggestions: ['Parle-moi davantage de ça', 'Et ensuite, qu’est-ce qui se passe ?'],
        showInterstitial: false,
      });
    } catch (fallbackError) {
      console.error('Chat fallback failed:', fallbackError.message);
      return res.status(502).json({ error: 'Le service IA est temporairement indisponible.' });
    }
  }
});

// Admin middleware
function requireAdmin(req, res, next) {
  if (req.user.role !== 'admin') {
    return res.status(403).json({ error: 'Accès réservé aux administrateurs.' });
  }
  next();
}

// Helper to get badge type
function getUserBadge(user) {
  const badge = attributeBadge(user);
  return badge;
}

// Character of the day - rotate daily
function getCharacterOfTheDay() {
  const today = utcDate();
  const dayOfYear = Math.floor((new Date(today) - new Date(new Date(today).getFullYear(), 0, 1)) / (1000 * 60 * 60 * 24));
  const systemChars = db.prepare('SELECT id FROM characters WHERE is_system = 1').all();
  const index = dayOfYear % systemChars.length;
  return systemChars[index]?.id || systemChars[0]?.id || null;
}

// ==================== SOCIAL ROUTES ====================

// POSTS
app.post('/api/posts', (req, res) => {
  const { postType, postSubtype, content, characterId, imageUrl, visibility } = req.body;
  if (!postType || !content || typeof content !== 'string') {
    return res.status(400).json({ error: 'postType et content requis.' });
  }
  
  const visibilityVal = visibility || 'public';
  if (!['public', 'followers', 'private'].includes(visibilityVal)) {
    return res.status(400).json({ error: 'Visibilité invalide.' });
  }
  
  const result = db.prepare(`
    INSERT INTO posts (user_id, post_type, post_subtype, content, character_id, image_url, visibility)
    VALUES (?, ?, ?, ?, ?, ?, ?)
  `).run(req.user.id, postType, postSubtype || null, content, characterId || null, imageUrl || null, visibilityVal);
  
  const postId = result.lastInsertRowid;
  
  // Create feed event for followers
  const followers = db.prepare('SELECT follower_id FROM follows WHERE following_id = ?').all(req.user.id);
  for (const follower of followers) {
    db.prepare('INSERT INTO feed_events (user_id, event_type, target_id) VALUES (?, ?, ?)')
      .run(follower.follower_id, 'new_post', postId);
  }
  
  // Create notification for admin if this is a feedback or update post
  if (['feedback', 'update', 'announcement'].includes(postType)) {
    const adminUser = db.prepare('SELECT id FROM users WHERE role = ?').get('admin');
    if (adminUser) {
      db.prepare(`
        INSERT INTO notifications (user_id, type, actor_id, target_type, target_id)
        VALUES (?, ?, ?, ?, ?)
      `).run(adminUser.id, 'official_post', req.user.id, 'post', postId);
    }
  }
  
  res.json({ id: postId });
});

app.get('/api/posts/:id', (req, res) => {
  const post = db.prepare(`
    SELECT p.*, u.display_name, u.avatar_url, u.username, u.badge_type,
           c.name as character_name, c.avatar_url as character_avatar_url
    FROM posts p
    LEFT JOIN users u ON p.user_id = u.id
    LEFT JOIN characters c ON p.character_id = c.id
    WHERE p.id = ?
  `).get(req.params.id);
  
  if (!post) return res.status(404).json({ error: 'Post introuvable.' });
  
  // Check visibility
  if (post.visibility === 'private' && post.user_id !== req.user.id) {
    return res.status(403).json({ error: 'Accès refusé.' });
  }
  if (post.visibility === 'followers' && post.user_id !== req.user.id) {
    const isFollowing = db.prepare('SELECT 1 FROM follows WHERE follower_id = ? AND following_id = ?')
      .get(req.user.id, post.user_id);
    if (!isFollowing) return res.status(403).json({ error: 'Accès refusé.' });
  }
  
  // Increment views if not the owner
  if (post.user_id !== req.user.id) {
    db.prepare('UPDATE posts SET views_count = COALESCE(views_count, 0) + 1 WHERE id = ?').run(post.id);
  }
  
  res.json(post);
});

app.delete('/api/posts/:id', (req, res) => {
  const post = db.prepare('SELECT user_id FROM posts WHERE id = ?').get(req.params.id);
  if (!post) return res.status(404).json({ error: 'Post introuvable.' });
  if (post.user_id !== req.user.id && req.user.role !== 'admin') {
    return res.status(403).json({ error: 'Accès refusé.' });
  }
  db.prepare('DELETE FROM posts WHERE id = ?').run(req.params.id);
  res.json({ success: true });
});

app.get('/api/feed', (req, res) => {
  const type = req.query.type || 'for_you';
  const page = parseInt(req.query.page) || 0;
  const limit = Math.min(parseInt(req.query.limit) || 20, 50);
  const offset = page * limit;
  
  let query, countQuery;
  const params = [];
  
  switch (type) {
    case 'following':
      query = `
        SELECT p.*, u.display_name, u.avatar_url, u.username, u.badge_type,
               c.name as character_name, c.avatar_url as character_avatar_url,
               pl.count as liked
        FROM posts p
        JOIN follows f ON p.user_id = f.following_id
        LEFT JOIN users u ON p.user_id = u.id
        LEFT JOIN characters c ON p.character_id = c.id
        LEFT JOIN (SELECT post_id, COUNT(*) as count FROM post_likes GROUP BY post_id) pl ON p.id = pl.post_id
        WHERE f.follower_id = ? AND p.visibility IN ('public', 'followers')
        ORDER BY p.is_pinned_global DESC, p.created_at DESC
        LIMIT ? OFFSET ?
      `;
      params.push(req.user.id, limit, offset);
      countQuery = `
        SELECT COUNT(*) as count
        FROM posts p
        JOIN follows f ON p.user_id = f.following_id
        WHERE f.follower_id = ? AND p.visibility IN ('public', 'followers')
      `;
      params.push(req.user.id);
      break;
      
    case 'recent':
      query = `
        SELECT p.*, u.display_name, u.avatar_url, u.username, u.badge_type,
               c.name as character_name, c.avatar_url as character_avatar_url,
               pl.count as liked
        FROM posts p
        LEFT JOIN users u ON p.user_id = u.id
        LEFT JOIN characters c ON p.character_id = c.id
        LEFT JOIN (SELECT post_id, COUNT(*) as count FROM post_likes GROUP BY post_id) pl ON p.id = pl.post_id
        WHERE p.visibility = 'public'
        ORDER BY p.is_pinned_global DESC, p.created_at DESC
        LIMIT ? OFFSET ?
      `;
      params.push(limit, offset);
      countQuery = `SELECT COUNT(*) as count FROM posts WHERE visibility = 'public'`;
      break;
      
    default: // for_you
      query = `
        SELECT p.*, u.display_name, u.avatar_url, u.username, u.badge_type,
               c.name as character_name, c.avatar_url as character_avatar_url,
               pl.count as liked
        FROM posts p
        LEFT JOIN users u ON p.user_id = u.id
        LEFT JOIN characters c ON p.character_id = c.id
        LEFT JOIN (SELECT post_id, COUNT(*) as count FROM post_likes GROUP BY post_id) pl ON p.id = pl.post_id
        WHERE p.visibility = 'public' AND (
          p.user_id IN (SELECT following_id FROM follows WHERE follower_id = ?) OR
          p.is_pinned_global = 1 OR
          p.user_id = ?
        )
        ORDER BY p.is_pinned_global DESC, 
                 CASE WHEN p.is_pinned_global = 1 THEN 1 ELSE 0 END DESC,
                 p.created_at DESC
        LIMIT ? OFFSET ?
      `;
      params.push(req.user.id, req.user.id, limit, offset);
      countQuery = `
        SELECT COUNT(*) as count
        FROM posts p
        WHERE p.visibility = 'public' AND (
          p.user_id IN (SELECT following_id FROM follows WHERE follower_id = ?) OR
          p.is_pinned_global = 1 OR
          p.user_id = ?
        )
      `;
      params.push(req.user.id, req.user.id);
  }
  
  const posts = db.prepare(query).all(...params.slice(0, -2 || undefined));
  const total = db.prepare(countQuery).get(...params.slice(-2)).count;
  
  res.json({ posts, total, page, hasMore: offset + posts.length < total });
});

// POST LIKES
app.post('/api/posts/:id/like', (req, res) => {
  const post = db.prepare('SELECT user_id FROM posts WHERE id = ?').get(req.params.id);
  if (!post) return res.status(404).json({ error: 'Post introuvable.' });
  
  const existingLike = db.prepare('SELECT 1 FROM post_likes WHERE post_id = ? AND user_id = ?')
    .get(req.params.id, req.user.id);
  
  if (existingLike) {
    db.prepare('DELETE FROM post_likes WHERE post_id = ? AND user_id = ?').run(req.params.id, req.user.id);
    db.prepare('UPDATE posts SET likes_count = MAX(0, likes_count - 1) WHERE id = ?').run(req.params.id);
    return res.json({ liked: false });
  }
  
  db.prepare('INSERT INTO post_likes (post_id, user_id) VALUES (?, ?)').run(req.params.id, req.user.id);
  db.prepare('UPDATE posts SET likes_count = likes_count + 1 WHERE id = ?').run(req.params.id);
  
  // Create notification for post author
  if (post.user_id !== req.user.id) {
    db.prepare(`
      INSERT INTO notifications (user_id, type, actor_id, target_type, target_id)
      VALUES (?, ?, ?, ?, ?)
    `).run(post.user_id, 'post_like', req.user.id, 'post', req.params.id);
  }
  
  res.json({ liked: true });
});

// POST COMMENTS
app.post('/api/posts/:id/comments', (req, res) => {
  const { content, parentCommentId } = req.body;
  if (!content || typeof content !== 'string') {
    return res.status(400).json({ error: 'content requis.' });
  }
  
  const post = db.prepare('SELECT user_id, visibility FROM posts WHERE id = ?').get(req.params.id);
  if (!post) return res.status(404).json({ error: 'Post introuvable.' });
  
  // Check visibility
  if (post.visibility === 'private' && post.user_id !== req.user.id) {
    return res.status(403).json({ error: 'Accès refusé.' });
  }
  if (post.visibility === 'followers' && post.user_id !== req.user.id) {
    const isFollowing = db.prepare('SELECT 1 FROM follows WHERE follower_id = ? AND following_id = ?')
      .get(req.user.id, post.user_id);
    if (!isFollowing) return res.status(403).json({ error: 'Accès refusé.' });
  }
  
  const result = db.prepare(`
    INSERT INTO post_comments (post_id, user_id, content, parent_comment_id)
    VALUES (?, ?, ?, ?)
  `).run(req.params.id, req.user.id, content, parentCommentId || null);
  
  const commentId = result.lastInsertRowid;
  db.prepare('UPDATE posts SET comments_count = comments_count + 1 WHERE id = ?').run(req.params.id);
  
  // Create notification for post author
  if (post.user_id !== req.user.id) {
    db.prepare(`
      INSERT INTO notifications (user_id, type, actor_id, target_type, target_id)
      VALUES (?, ?, ?, ?, ?)
    `).run(post.user_id, 'post_comment', req.user.id, 'post', req.params.id);
  }
  
  // Create notification for parent comment author if replying
  if (parentCommentId) {
    const parentComment = db.prepare('SELECT user_id FROM post_comments WHERE id = ?').get(parentCommentId);
    if (parentComment && parentComment.user_id !== req.user.id) {
      db.prepare(`
        INSERT INTO notifications (user_id, type, actor_id, target_type, target_id)
        VALUES (?, ?, ?, ?, ?)
      `).run(parentComment.user_id, 'comment_reply', req.user.id, 'comment', commentId);
    }
  }
  
  res.json({ id: commentId });
});

app.get('/api/posts/:id/comments', (req, res) => {
  const page = parseInt(req.query.page) || 0;
  const limit = Math.min(parseInt(req.query.limit) || 20, 50);
  const offset = page * limit;
  
  const comments = db.prepare(`
    SELECT pc.*, u.display_name, u.avatar_url, u.username, u.badge_type,
           cl.count as likes_count,
           (SELECT 1 FROM comment_likes WHERE comment_id = pc.id AND user_id = ?) as liked_by_me
    FROM post_comments pc
    LEFT JOIN users u ON pc.user_id = u.id
    LEFT JOIN (SELECT comment_id, COUNT(*) as count FROM comment_likes GROUP BY comment_id) cl ON pc.id = cl.comment_id
    WHERE pc.post_id = ?
    ORDER BY pc.created_at DESC
    LIMIT ? OFFSET ?
  `).all(req.user.id, req.params.id, limit, offset);
  
  const total = db.prepare('SELECT COUNT(*) as count FROM post_comments WHERE post_id = ?')
    .get(req.params.id).count;
  
  res.json({ comments, total, page, hasMore: offset + comments.length < total });
});

app.delete('/api/comments/:id', (req, res) => {
  const comment = db.prepare('SELECT user_id, post_id FROM post_comments WHERE id = ?').get(req.params.id);
  if (!comment) return res.status(404).json({ error: 'Commentaire introuvable.' });
  if (comment.user_id !== req.user.id && req.user.role !== 'admin') {
    return res.status(403).json({ error: 'Accès refusé.' });
  }
  db.prepare('DELETE FROM post_comments WHERE id = ?').run(req.params.id);
  db.prepare('UPDATE posts SET comments_count = MAX(0, comments_count - 1) WHERE id = ?').run(comment.post_id);
  res.json({ success: true });
});

app.post('/api/comments/:id/like', (req, res) => {
  const comment = db.prepare('SELECT user_id FROM post_comments WHERE id = ?').get(req.params.id);
  if (!comment) return res.status(404).json({ error: 'Commentaire introuvable.' });
  
  const existingLike = db.prepare('SELECT 1 FROM comment_likes WHERE comment_id = ? AND user_id = ?')
    .get(req.params.id, req.user.id);
  
  if (existingLike) {
    db.prepare('DELETE FROM comment_likes WHERE comment_id = ? AND user_id = ?').run(req.params.id, req.user.id);
    db.prepare('UPDATE post_comments SET likes_count = MAX(0, likes_count - 1) WHERE id = ?').run(req.params.id);
    return res.json({ liked: false });
  }
  
  db.prepare('INSERT INTO comment_likes (comment_id, user_id) VALUES (?, ?)').run(req.params.id, req.user.id);
  db.prepare('UPDATE post_comments SET likes_count = likes_count + 1 WHERE id = ?').run(req.params.id);
  
  // Create notification
  if (comment.user_id !== req.user.id) {
    db.prepare(`
      INSERT INTO notifications (user_id, type, actor_id, target_type, target_id)
      VALUES (?, ?, ?, ?, ?)
    `).run(comment.user_id, 'comment_like', req.user.id, 'comment', req.params.id);
  }
  
  res.json({ liked: true });
});

// FOLLOWS
app.post('/api/users/:id/follow', (req, res) => {
  const targetUser = db.prepare('SELECT id FROM users WHERE id = ?').get(req.params.id);
  if (!targetUser) return res.status(404).json({ error: 'Utilisateur introuvable.' });
  if (targetUser.id === req.user.id) return res.status(400).json({ error: 'Impossible de se suivre soi-même.' });
  
  const existingFollow = db.prepare('SELECT 1 FROM follows WHERE follower_id = ? AND following_id = ?')
    .get(req.user.id, targetUser.id);
  
  if (existingFollow) {
    db.prepare('DELETE FROM follows WHERE follower_id = ? AND following_id = ?').run(req.user.id, targetUser.id);
    return res.json({ following: false });
  }
  
  db.prepare('INSERT INTO follows (follower_id, following_id) VALUES (?, ?)').run(req.user.id, targetUser.id);
  
  // Create notification
  db.prepare(`
    INSERT INTO notifications (user_id, type, actor_id, target_type, target_id)
    VALUES (?, ?, ?, ?, ?)
  `).run(targetUser.id, 'follow', req.user.id, 'user', targetUser.id);
  
  res.json({ following: true });
});

app.get('/api/users/:id/followers', (req, res) => {
  const page = parseInt(req.query.page) || 0;
  const limit = Math.min(parseInt(req.query.limit) || 20, 50);
  const offset = page * limit;
  
  const followers = db.prepare(`
    SELECT u.id, u.display_name, u.avatar_url, u.username, u.badge_type,
           (SELECT 1 FROM follows WHERE follower_id = ? AND following_id = u.id) as followed_by_me
    FROM follows f
    JOIN users u ON f.follower_id = u.id
    WHERE f.following_id = ?
    ORDER BY f.created_at DESC
    LIMIT ? OFFSET ?
  `).all(req.user.id, req.params.id, limit, offset);
  
  const total = db.prepare('SELECT COUNT(*) as count FROM follows WHERE following_id = ?')
    .get(req.params.id).count;
  
  res.json({ followers, total, page, hasMore: offset + followers.length < total });
});

app.get('/api/users/:id/following', (req, res) => {
  const page = parseInt(req.query.page) || 0;
  const limit = Math.min(parseInt(req.query.limit) || 20, 50);
  const offset = page * limit;
  
  const following = db.prepare(`
    SELECT u.id, u.display_name, u.avatar_url, u.username, u.badge_type,
           (SELECT 1 FROM follows WHERE follower_id = ? AND following_id = u.id) as followed_by_me
    FROM follows f
    JOIN users u ON f.following_id = u.id
    WHERE f.follower_id = ?
    ORDER BY f.created_at DESC
    LIMIT ? OFFSET ?
  `).all(req.user.id, req.params.id, limit, offset);
  
  const total = db.prepare('SELECT COUNT(*) as count FROM follows WHERE follower_id = ?')
    .get(req.params.id).count;
  
  res.json({ following, total, page, hasMore: offset + following.length < total });
});

// USER PROFILE
app.get('/api/users/:id', (req, res) => {
  const user = db.prepare(`
    SELECT u.*, p.display_name as profile_display_name, p.bio as profile_bio, p.avatar_url as profile_avatar_url
    FROM users u
    LEFT JOIN profiles p ON u.id = p.user_id
    WHERE u.id = ?
  `).get(req.params.id);
  
  if (!user) return res.status(404).json({ error: 'Utilisateur introuvable.' });
  
  const isFollowing = db.prepare('SELECT 1 FROM follows WHERE follower_id = ? AND following_id = ?')
    .get(req.user.id, user.id);
  
  const followerCount = db.prepare('SELECT COUNT(*) as count FROM follows WHERE following_id = ?')
    .get(user.id).count;
  const followingCount = db.prepare('SELECT COUNT(*) as count FROM follows WHERE follower_id = ?')
    .get(user.id).count;
  const postCount = db.prepare('SELECT COUNT(*) as count FROM posts WHERE user_id = ? AND visibility = ?')
    .get(user.id, 'public').count;
  
  res.json({
    ...user,
    isFollowing: Boolean(isFollowing),
    followerCount,
    followingCount,
    postCount,
    badge: getUserBadge(user)
  });
});

app.get('/api/users/:id/posts', (req, res) => {
  const userId = Number(req.params.id);
  const page = parseInt(req.query.page) || 0;
  const limit = Math.min(parseInt(req.query.limit) || 20, 50);
  const offset = page * limit;

  const posts = db.prepare(`
    SELECT p.*, u.display_name, u.avatar_url, u.username, u.badge_type,
           (SELECT 1 FROM post_likes WHERE post_id = p.id AND user_id = ?) as liked_by_me
    FROM posts p
    JOIN users u ON p.user_id = u.id
    WHERE p.user_id = ?
    ORDER BY p.created_at DESC
    LIMIT ? OFFSET ?
  `).all(req.user.id, userId, limit, offset);

  const total = db.prepare('SELECT COUNT(*) as count FROM posts WHERE user_id = ?')
    .get(userId)?.count || 0;

  res.json({ posts, total, page, hasMore: offset + posts.length < total });
});

// NOTIFICATIONS
app.get('/api/notifications', (req, res) => {
  const page = parseInt(req.query.page) || 0;
  const limit = Math.min(parseInt(req.query.limit) || 20, 50);
  const offset = page * limit;
  
  const notifications = db.prepare(`
    SELECT n.*, u.display_name, u.avatar_url, u.username, u.badge_type,
           p.post_type, p.content as post_content
    FROM notifications n
    LEFT JOIN users u ON n.actor_id = u.id
    LEFT JOIN posts p ON n.target_type = 'post' AND n.target_id = p.id
    WHERE n.user_id = ?
    ORDER BY n.created_at DESC
    LIMIT ? OFFSET ?
  `).all(req.user.id, limit, offset);
  
  const total = db.prepare('SELECT COUNT(*) as count FROM notifications WHERE user_id = ?')
    .get(req.user.id).count;
  
  res.json({ notifications, total, page, hasMore: offset + notifications.length < total });
});

app.post('/api/notifications/:id/read', (req, res) => {
  db.prepare('UPDATE notifications SET read = 1 WHERE id = ? AND user_id = ?').run(req.params.id, req.user.id);
  res.json({ success: true });
});

app.post('/api/notifications/read-all', (req, res) => {
  db.prepare('UPDATE notifications SET read = 1 WHERE user_id = ?').run(req.user.id);
  res.json({ success: true });
});

app.get('/api/notifications/unread-count', (req, res) => {
  const count = db.prepare('SELECT COUNT(*) as count FROM notifications WHERE user_id = ? AND read = 0')
    .get(req.user.id).count;
  res.json({ count });
});

// REPORTS
app.post('/api/reports', (req, res) => {
  const { targetType, targetId, reason } = req.body;
  if (!targetType || !targetId || !reason) {
    return res.status(400).json({ error: 'targetType, targetId et reason requis.' });
  }
  
  db.prepare(`
    INSERT INTO reports (reporter_id, target_type, target_id, reason)
    VALUES (?, ?, ?, ?)
  `).run(req.user.id, targetType, targetId, reason);
  
  res.json({ success: true });
});

// FEEDBACK
app.post('/api/feedback', (req, res) => {
  const { category, content, contactEmail } = req.body;
  if (!category || !content) {
    return res.status(400).json({ error: 'category et content requis.' });
  }
  
  db.prepare(`
    INSERT INTO feedback (user_id, category, content, contact_email)
    VALUES (?, ?, ?, ?)
  `).run(req.user.id, category, content, contactEmail || null);
  
  // Create notification for admin
  const adminUser = db.prepare('SELECT id FROM users WHERE role = ?').get('admin');
  if (adminUser) {
    db.prepare(`
      INSERT INTO notifications (user_id, type, actor_id)
      VALUES (?, ?, ?)
    `).run(adminUser.id, 'new_feedback', req.user.id);
  }
  
  res.json({ success: true });
});

// SEARCH
app.get('/api/social/search', (req, res) => {
  const query = req.query.q;
  const type = req.query.type || 'all';
  const page = parseInt(req.query.page) || 0;
  const limit = Math.min(parseInt(req.query.limit) || 20, 50);
  const offset = page * limit;
  
  if (!query || query.length < 2) {
    return res.status(400).json({ error: 'La requête doit contenir au moins 2 caractères.' });
  }
  
  const searchPattern = `%${query}%`;
  let results, total;
  
  switch (type) {
    case 'users':
      results = db.prepare(`
        SELECT u.id, u.display_name, u.avatar_url, u.username, u.badge_type, p.bio
        FROM users u
        LEFT JOIN profiles p ON u.id = p.user_id
        WHERE u.display_name LIKE ? OR u.username LIKE ? OR p.bio LIKE ?
        ORDER BY u.display_name
        LIMIT ? OFFSET ?
      `).all(searchPattern, searchPattern, searchPattern, limit, offset);
      total = db.prepare(`
        SELECT COUNT(*) as count
        FROM users u
        LEFT JOIN profiles p ON u.id = p.user_id
        WHERE u.display_name LIKE ? OR u.username LIKE ? OR p.bio LIKE ?
      `).get(searchPattern, searchPattern, searchPattern).count;
      break;
      
    case 'posts':
      results = db.prepare(`
        SELECT p.*, u.display_name, u.avatar_url, u.username, u.badge_type
        FROM posts p
        JOIN users u ON p.user_id = u.id
        WHERE p.content LIKE ? AND p.visibility = 'public'
        ORDER BY p.created_at DESC
        LIMIT ? OFFSET ?
      `).all(searchPattern, limit, offset);
      total = db.prepare(`
        SELECT COUNT(*) as count
        FROM posts p
        WHERE p.content LIKE ? AND p.visibility = 'public'
      `).get(searchPattern).count;
      break;
      
    case 'characters':
      results = db.prepare(`
        SELECT c.*, u.display_name as creator_name, u.username as creator_username
        FROM characters c
        LEFT JOIN users u ON c.user_id = u.id
        WHERE c.is_public = 1 AND (c.name LIKE ? OR c.system_prompt LIKE ? OR c.greeting LIKE ?)
        ORDER BY c.likes_count DESC
        LIMIT ? OFFSET ?
      `).all(searchPattern, searchPattern, searchPattern, limit, offset);
      total = db.prepare(`
        SELECT COUNT(*) as count
        FROM characters c
        WHERE c.is_public = 1 AND (c.name LIKE ? OR c.system_prompt LIKE ? OR c.greeting LIKE ?)
      `).get(searchPattern, searchPattern, searchPattern).count;
      break;
      
    default:
      // Search all
      const users = db.prepare(`
        SELECT 'user' as type, u.id, u.display_name as name, u.avatar_url, u.username, p.bio as description
        FROM users u
        LEFT JOIN profiles p ON u.id = p.user_id
        WHERE u.display_name LIKE ? OR u.username LIKE ? OR p.bio LIKE ?
        LIMIT 5
      `).all(searchPattern, searchPattern, searchPattern);
      
      const posts = db.prepare(`
        SELECT 'post' as type, p.id, p.content as name, NULL as avatar_url, u.username,
               SUBSTR(p.content, 1, 100) as description
        FROM posts p
        JOIN users u ON p.user_id = u.id
        WHERE p.content LIKE ? AND p.visibility = 'public'
        LIMIT 5
      `).all(searchPattern);
      
      const characters = db.prepare(`
        SELECT 'character' as type, c.id, c.name, c.avatar_url, u.username,
               c.greeting as description
        FROM characters c
        LEFT JOIN users u ON c.user_id = u.id
        WHERE c.is_public = 1 AND (c.name LIKE ? OR c.greeting LIKE ?)
        LIMIT 5
      `).all(searchPattern, searchPattern);
      
      results = [...users, ...posts, ...characters].slice(0, limit);
      total = users.length + posts.length + characters.length;
  }
  
  res.json({ results, total, page, hasMore: offset + results.length < total });
});

// POLLS
app.post('/api/posts/:id/poll', (req, res) => {
  const { question, options, endsAt } = req.body;
  if (!question || !options || !Array.isArray(options) || options.length < 2) {
    return res.status(400).json({ error: 'question et options (2+) requis.' });
  }
  
  const post = db.prepare('SELECT user_id FROM posts WHERE id = ?').get(req.params.id);
  if (!post) return res.status(404).json({ error: 'Post introuvable.' });
  if (post.user_id !== req.user.id && req.user.role !== 'admin') {
    return res.status(403).json({ error: 'Accès refusé.' });
  }
  
  db.prepare(`
    INSERT INTO post_polls (post_id, question, options, ends_at)
    VALUES (?, ?, ?, ?)
  `).run(req.params.id, question, JSON.stringify(options), endsAt || null);
  
  res.json({ success: true });
});

app.post('/api/posts/:id/vote', (req, res) => {
  const { optionIndex } = req.body;
  if (optionIndex === undefined || optionIndex < 0) {
    return res.status(400).json({ error: 'optionIndex requis.' });
  }
  
  const poll = db.prepare(`
    SELECT pp.id, pp.post_id, pp.options, pp.ends_at
    FROM post_polls pp
    WHERE pp.post_id = ?
  `).get(req.params.id);
  
  if (!poll) return res.status(404).json({ error: 'Sondage introuvable.' });
  
  if (poll.ends_at && new Date(poll.ends_at) < new Date()) {
    return res.status(400).json({ error: 'Ce sondage est terminé.' });
  }
  
  const options = JSON.parse(poll.options);
  if (optionIndex >= options.length) {
    return res.status(400).json({ error: 'Option invalide.' });
  }
  
  const existingVote = db.prepare('SELECT 1 FROM post_poll_votes WHERE poll_id = ? AND user_id = ?')
    .get(poll.id, req.user.id);
  
  if (existingVote) {
    return res.status(400).json({ error: 'Vous avez déjà voté.' });
  }
  
  db.prepare('INSERT INTO post_poll_votes (poll_id, user_id, option_index) VALUES (?, ?, ?)')
    .run(poll.id, req.user.id, optionIndex);
  
  res.json({ success: true });
});

app.get('/api/posts/:id/poll-results', (req, res) => {
  const poll = db.prepare(`
    SELECT pp.id, pp.question, pp.options, pp.ends_at
    FROM post_polls pp
    WHERE pp.post_id = ?
  `).get(req.params.id);
  
  if (!poll) return res.status(404).json({ error: 'Sondage introuvable.' });
  
  const votes = db.prepare('SELECT option_index, COUNT(*) as count FROM post_poll_votes WHERE poll_id = ? GROUP BY option_index')
    .all(poll.id);
  
  const options = JSON.parse(poll.options);
  const results = options.map((option, index) => ({
    option,
    votes: votes.find(v => v.option_index === index)?.count || 0
  }));
  
  const totalVotes = results.reduce((sum, r) => sum + r.votes, 0);
  
  res.json({ question: poll.question, results, totalVotes, ended: poll.ends_at && new Date(poll.ends_at) < new Date() });
});

// SHARED CHARACTER
app.get('/api/shared/:token', (req, res) => {
  const share = db.prepare(`
    SELECT s.*, c.*, u.display_name as creator_name, u.username as creator_username
    FROM shares s
    JOIN characters c ON s.character_id = c.id
    JOIN users u ON s.user_id = u.id
    WHERE s.token = ?
  `).get(req.params.token);
  
  if (!share) return res.status(404).json({ error: 'Partage introuvable ou expiré.' });
  
  // Increment share count
  db.prepare('UPDATE posts SET shares_count = COALESCE(shares_count, 0) + 1 WHERE character_id = ?')
    .run(share.character_id);
  
  res.json({
    character: {
      id: share.id,
      name: share.name,
      system_prompt: share.system_prompt,
      greeting: share.greeting,
      avatar_url: share.avatar_url,
      is_nsfw: Boolean(share.is_nsfw),
      is_public: Boolean(share.is_public),
      scenario_goal: share.scenario_goal,
      appearance: share.appearance,
      boundaries: share.boundaries,
      tts_voice: share.tts_voice,
      banned_words: share.banned_words
    },
    creator: {
      displayName: share.creator_name,
      username: share.creator_username
    }
  });
});

app.post('/api/shared/:token/import', (req, res) => {
  const share = db.prepare('SELECT character_id FROM shares WHERE token = ?').get(req.params.token);
  if (!share) return res.status(404).json({ error: 'Partage introuvable ou expiré.' });
  
  const originalCharacter = db.prepare('SELECT * FROM characters WHERE id = ?').get(share.character_id);
  if (!originalCharacter) return res.status(404).json({ error: 'Personnage introuvable.' });
  
  // Create a copy for the current user
  const result = db.prepare(`
    INSERT INTO characters (user_id, name, system_prompt, greeting, avatar_url, is_nsfw, 
                           scenario_goal, scenario_steps, boundaries, appearance, tts_voice, 
                           banned_words, proactive_enabled, silent_mode_enabled)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0)
  `).run(
    req.user.id,
    `${originalCharacter.name} (copie)`,
    originalCharacter.system_prompt,
    originalCharacter.greeting,
    originalCharacter.avatar_url,
    originalCharacter.is_nsfw,
    originalCharacter.scenario_goal,
    originalCharacter.scenario_steps,
    originalCharacter.boundaries,
    originalCharacter.appearance,
    originalCharacter.tts_voice,
    originalCharacter.banned_words
  );
  
  const newCharacterId = result.lastInsertRowid;
  
  // Copy seed memory if exists
  const seedMemory = db.prepare('SELECT fact FROM character_seed_memory WHERE character_id = ?')
    .all(originalCharacter.id);
  for (const row of seedMemory) {
    db.prepare('INSERT INTO character_seed_memory (character_id, fact) VALUES (?, ?)')
      .run(newCharacterId, row.fact);
  }
  
  res.json({ id: newCharacterId });
});

// CHARACTER OF THE DAY
app.get('/api/character-of-the-day', (req, res) => {
  const charId = getCharacterOfTheDay();
  if (!charId) return res.status(404).json({ error: 'Aucun personnage disponible.' });
  
  const character = db.prepare('SELECT * FROM characters WHERE id = ?').get(charId);
  res.json(character);
});

// BADGE
app.get('/api/users/:id/badge', (req, res) => {
  const user = db.prepare('SELECT * FROM users WHERE id = ?').get(req.params.id);
  if (!user) return res.status(404).json({ error: 'Utilisateur introuvable.' });
  
  const badge = getUserBadge(user);
  res.json({ badge });
});

// ==================== ADMIN ROUTES ====================

// Pin/Unpin global post
app.post('/api/admin/posts/:id/pin-global', requireAdmin, (req, res) => {
  const post = db.prepare('SELECT id FROM posts WHERE id = ?').get(req.params.id);
  if (!post) return res.status(404).json({ error: 'Post introuvable.' });
  
  db.prepare('UPDATE posts SET is_pinned_global = 1 WHERE id = ?').run(req.params.id);
  res.json({ success: true });
});

app.post('/api/admin/posts/:id/unpin-global', requireAdmin, (req, res) => {
  const post = db.prepare('SELECT id FROM posts WHERE id = ?').get(req.params.id);
  if (!post) return res.status(404).json({ error: 'Post introuvable.' });
  
  db.prepare('UPDATE posts SET is_pinned_global = 0 WHERE id = ?').run(req.params.id);
  res.json({ success: true });
});

// Reports management
app.get('/api/admin/reports', requireAdmin, (req, res) => {
  const status = req.query.status || 'pending';
  const page = parseInt(req.query.page) || 0;
  const limit = Math.min(parseInt(req.query.limit) || 20, 50);
  const offset = page * limit;
  
  const reports = db.prepare(`
    SELECT r.*, u.display_name as reporter_name, u.username as reporter_username
    FROM reports r
    JOIN users u ON r.reporter_id = u.id
    WHERE r.status = ?
    ORDER BY r.created_at DESC
    LIMIT ? OFFSET ?
  `).all(status, limit, offset);
  
  const total = db.prepare('SELECT COUNT(*) as count FROM reports WHERE status = ?')
    .get(status).count;
  
  res.json({ reports, total, page, hasMore: offset + reports.length < total });
});

app.post('/api/admin/reports/:id/resolve', requireAdmin, (req, res) => {
  const { action, duration, reason } = req.body;
  const report = db.prepare('SELECT * FROM reports WHERE id = ?').get(req.params.id);
  if (!report) return res.status(404).json({ error: 'Signalement introuvable.' });
  
  const validActions = ['dismiss', 'remove_content', 'warn_user', 'ban_user'];
  if (!validActions.includes(action)) {
    return res.status(400).json({ error: 'Action invalide.' });
  }
  
  const updateReport = db.prepare('UPDATE reports SET status = ? WHERE id = ?').run('resolved', req.params.id);
  
  switch (action) {
    case 'remove_content':
      if (report.target_type === 'post') {
        db.prepare('DELETE FROM posts WHERE id = ?').run(report.target_id);
      } else if (report.target_type === 'comment') {
        db.prepare('DELETE FROM post_comments WHERE id = ?').run(report.target_id);
      }
      break;
      
    case 'warn_user':
      // Create warning notification
      db.prepare(`
        INSERT INTO notifications (user_id, type, description)
        VALUES (?, ?, ?)
      `).run(report.reporter_id, 'warning', reason || 'Avertissement de la modération');
      break;
      
    case 'ban_user':
      const banDuration = duration || '7d';
      const banUntil = new Date();
      if (banDuration === 'permanent') {
        banUntil.setFullYear(banUntil.getFullYear() + 100);
      } else if (banDuration.endsWith('d')) {
        banUntil.setDate(banUntil.getDate() + parseInt(banDuration));
      } else if (banDuration.endsWith('h')) {
        banUntil.setHours(banUntil.getHours() + parseInt(banDuration));
      }
      
      db.prepare(`
        UPDATE users SET banned_until = ?, role = 'banned'
        WHERE id = ?
      `).run(banUntil.toISOString(), report.reporter_id);
      break;
  }
  
  res.json({ success: true });
});

// User management
app.post('/api/admin/users/:id/ban', requireAdmin, (req, res) => {
  const { duration, reason } = req.body;
  const user = db.prepare('SELECT id FROM users WHERE id = ?').get(req.params.id);
  if (!user) return res.status(404).json({ error: 'Utilisateur introuvable.' });
  
  const banUntil = new Date();
  if (duration === 'permanent') {
    banUntil.setFullYear(banUntil.getFullYear() + 100);
  } else if (duration?.endsWith('d')) {
    banUntil.setDate(banUntil.getDate() + parseInt(duration));
  } else if (duration?.endsWith('h')) {
    banUntil.setHours(banUntil.getHours() + parseInt(duration));
  } else {
    banUntil.setDate(banUntil.getDate() + 7);
  }
  
  db.prepare(`
    UPDATE users SET banned_until = ?, role = 'banned'
    WHERE id = ?
  `).run(banUntil.toISOString(), req.params.id);
  
  res.json({ success: true });
});

app.post('/api/admin/users/:id/unban', requireAdmin, (req, res) => {
  const user = db.prepare('SELECT id FROM users WHERE id = ?').get(req.params.id);
  if (!user) return res.status(404).json({ error: 'Utilisateur introuvable.' });
  
  db.prepare(`
    UPDATE users SET banned_until = NULL, role = 'user'
    WHERE id = ?
  `).run(req.params.id);
  
  res.json({ success: true });
});

app.post('/api/admin/users/:id/warn', requireAdmin, (req, res) => {
  const { reason } = req.body;
  const user = db.prepare('SELECT id FROM users WHERE id = ?').get(req.params.id);
  if (!user) return res.status(404).json({ error: 'Utilisateur introuvable.' });
  
  db.prepare(`
    INSERT INTO notifications (user_id, type, description)
    VALUES (?, ?, ?)
  `).run(user.id, 'warning', reason || 'Avertissement de l\'administration');
  
  res.json({ success: true });
});

// Feedback management
app.get('/api/admin/feedback', requireAdmin, (req, res) => {
  const status = req.query.status || 'new';
  const page = parseInt(req.query.page) || 0;
  const limit = Math.min(parseInt(req.query.limit) || 20, 50);
  const offset = page * limit;
  
  const feedbacks = db.prepare(`
    SELECT f.*, u.display_name, u.username, u.badge_type
    FROM feedback f
    JOIN users u ON f.user_id = u.id
    WHERE f.status = ?
    ORDER BY f.created_at DESC
    LIMIT ? OFFSET ?
  `).all(status, limit, offset);
  
  const total = db.prepare('SELECT COUNT(*) as count FROM feedback WHERE status = ?')
    .get(status).count;
  
  res.json({ feedbacks, total, page, hasMore: offset + feedbacks.length < total });
});

app.post('/api/admin/feedback/:id/resolve', requireAdmin, (req, res) => {
  const { response } = req.body;
  const feedback = db.prepare('SELECT * FROM feedback WHERE id = ?').get(req.params.id);
  if (!feedback) return res.status(404).json({ error: 'Feedback introuvable.' });
  
  db.prepare('UPDATE feedback SET status = ?, response = ? WHERE id = ?')
    .run('resolved', response || null, req.params.id);
  
  res.json({ success: true });
});

// Global stats
app.get('/api/admin/stats', requireAdmin, (req, res) => {
  const totalUsers = db.prepare('SELECT COUNT(*) as count FROM users').get().count;
  const activeUsers = db.prepare('SELECT COUNT(*) as count FROM users WHERE last_daily_claim >= date("now", "-7 days")').get().count;
  const totalMessages = db.prepare('SELECT COUNT(*) as count FROM messages').get().count;
  const totalCharacters = db.prepare('SELECT COUNT(*) as count FROM characters WHERE is_system = 0').get().count;
  const totalPosts = db.prepare('SELECT COUNT(*) as count FROM posts').get().count;
  const premiumUsers = db.prepare('SELECT COUNT(*) as count FROM users WHERE subscription_tier IN ("premium", "premium_plus")').get().count;
  const dailyActive = db.prepare('SELECT COUNT(DISTINCT user_id) as count FROM messages WHERE created_at >= date("now")').get().count;
  
  const piecesDistributed = db.prepare(`
    SELECT SUM(amount) as total FROM piece_transactions WHERE amount > 0 AND created_at >= date('now', '-30 days')
  `).get().total || 0;
  
  res.json({
    totalUsers,
    activeUsers,
    totalMessages,
    totalCharacters,
    totalPosts,
    premiumUsers,
    dailyActive,
    piecesDistributed
  });
});

app.use((error, req, res, next) => {
  if (error instanceof SyntaxError && 'body' in error) {
    return res.status(400).json({ error: 'JSON invalide.' });
  }
  console.error('Unhandled request error:', error.message);
  res.status(500).json({ error: 'Erreur serveur.' });
});

const port = Number(process.env.PORT) || 3000;
app.listen(port, '0.0.0.0', () => {
  console.log(`Pièces IA backend listening on port ${port}`);
});
