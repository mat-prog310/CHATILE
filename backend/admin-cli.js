import 'dotenv/config';
import Database from 'better-sqlite3';
import inquirer from 'inquirer';
import chalk from 'chalk';
import Table from 'cli-table3';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const databasePath = resolve(__dirname, 'data', 'pieces.sqlite');

if (!process.env.ADMIN_PASSWORD) {
  console.error(chalk.red('ERREUR : ADMIN_PASSWORD manquante dans backend/.env'));
  process.exit(1);
}

const db = new Database(databasePath);
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

// Initialize admin_logs table
db.exec(`
  CREATE TABLE IF NOT EXISTS admin_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    admin_action TEXT NOT NULL,
    target_id TEXT,
    target_type TEXT,
    details TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
  );
`);

// Add deleted_at to tables if missing for soft deletes
const addDeletedAtColumn = (tableName) => {
  const info = db.pragma(`table_info(${tableName})`);
  if (!info.some(col => col.name === 'deleted_at')) {
    db.exec(`ALTER TABLE ${tableName} ADD COLUMN deleted_at TEXT;`);
  }
};
['users', 'characters', 'stories', 'posts', 'post_comments'].forEach(addDeletedAtColumn);

function logAction(action, targetId, targetType, details = {}) {
  db.prepare(`
    INSERT INTO admin_logs (admin_action, target_id, target_type, details)
    VALUES (?, ?, ?, ?)
  `).run(action, String(targetId), targetType, JSON.stringify(details));
}

async function login() {
  const { password } = await inquirer.prompt([{
    type: 'password',
    name: 'password',
    message: 'Mot de passe admin :',
    mask: '*'
  }]);

  if (password !== process.env.ADMIN_PASSWORD) {
    console.error(chalk.red('Mot de passe incorrect.'));
    process.exit(1);
  }
  console.log(chalk.green('Connexion réussie !'));
}

async function dashboard() {
  console.log(chalk.blue('\n=== 📊 Tableau de bord ==='));

  const totalUsers = db.prepare('SELECT count(*) as c FROM users WHERE deleted_at IS NULL').get().c;
  const activeUsers = db.prepare("SELECT count(*) as c FROM users WHERE deleted_at IS NULL AND (last_streak_date >= date('now', '-7 days') OR created_at >= date('now', '-7 days'))").get().c;
  const totalChars = db.prepare('SELECT count(*) as c FROM characters WHERE deleted_at IS NULL').get().c;
  const totalStories = db.prepare('SELECT count(*) as c FROM stories WHERE deleted_at IS NULL').get().c;
  const pendingReports = db.prepare("SELECT count(*) as c FROM reports WHERE status = 'pending'").get().c;
  const totalPieces = db.prepare('SELECT sum(pieces) as s FROM users WHERE deleted_at IS NULL').get().s || 0;
  const premiumUsers = db.prepare("SELECT count(*) as c FROM users WHERE deleted_at IS NULL AND subscription_tier IN ('premium', 'premium_plus')").get().c;

  const table = new Table({
    head: [chalk.cyan('Statistique'), chalk.cyan('Valeur')],
    colWidths: [40, 20]
  });

  table.push(
    ['Total utilisateurs', totalUsers],
    ['Utilisateurs actifs (7j)', activeUsers],
    ['Total personnages', totalChars],
    ['Total histoires', totalStories],
    ['Signalements en attente', chalk.yellow(pendingReports)],
    ['Total Pièces en circulation', chalk.magenta(totalPieces)],
    ['Abonnés Premium/Premium+', premiumUsers]
  );

  console.log(table.toString());
  await inquirer.prompt([{ type: 'input', name: 'continue', message: 'Appuyez sur Entrée pour continuer...' }]);
}

async function mainMenu() {
  while (true) {
    console.log('');
    const { action } = await inquirer.prompt([{
      type: 'list',
      name: 'action',
      message: chalk.bold('=== CHATILE ADMIN ==='),
      choices: [
        '📊 Tableau de bord (stats globales)',
        '👥 Gérer les utilisateurs',
        '🤖 Gérer les personnages',
        '📖 Gérer les histoires',
        '🚨 Modération (signalements)',
        '💰 Économie (Pièces, achats)',
        '🚪 Quitter'
      ],
      pageSize: 10
    }]);

    switch (action) {
      case '📊 Tableau de bord (stats globales)':
        await dashboard();
        break;
      case '👥 Gérer les utilisateurs':
        await manageUsers();
        break;
      case '🤖 Gérer les personnages':
        await manageCharacters();
        break;
      case '📖 Gérer les histoires':
        await manageStories();
        break;
      case '🚨 Modération (signalements)':
        await manageReports();
        break;
      case '💰 Économie (Pièces, achats)':
        await manageEconomy();
        break;
      case '🚪 Quitter':
        console.log(chalk.green('Au revoir !'));
        process.exit(0);
    }
  }
}

// ... Placeholder functions for the rest ...
async function selectAndManageUser(users) {
  if (users.length === 0) return;

  const choices = users.map(u => ({
    name: `${u.id} | ${u.username} | ${u.subscription_tier} | ${u.deleted_at ? chalk.red('(Supprimé)') : u.banned_until ? chalk.red('(Banni)') : ''}`,
    value: u
  }));
  choices.push({ name: 'Retour', value: null });

  const { selected } = await inquirer.prompt([{
    type: 'list', name: 'selected', message: 'Sélectionnez un utilisateur :', choices, pageSize: 15
  }]);

  if (!selected) return;
  await userDetailMenu(selected.id);
}

async function userDetailMenu(userId) {
  while (true) {
    const u = db.prepare("SELECT * FROM users WHERE id = ?").get(userId);
    if (!u) {
      console.log(chalk.red('Utilisateur introuvable.'));
      return;
    }

    const charCount = db.prepare('SELECT count(*) as c FROM characters WHERE user_id = ?').get(u.id).c;
    const storyCount = db.prepare('SELECT count(*) as c FROM stories WHERE user_id = ?').get(u.id).c || 0;
    const reportCount = db.prepare("SELECT count(*) as c FROM reports WHERE target_type = 'user' AND target_id = ?").get(u.id).c;

    console.log(chalk.blue('\n=== Détail de l\'utilisateur ==='));
    const table = new Table({ colWidths: [30, 50] });
    table.push(
      ['ID', u.id],
      ['Username', u.username],
      ['Firebase UID', u.firebase_uid || '-'],
      ['Date inscription', u.created_at],
      ['Solde Pièces', u.pieces],
      ['Abonnement', u.subscription_tier],
      ['Personnages créés', charCount],
      ['Histoires créées', storyCount],
      ['Signalements reçus', reportCount],
      ['Statut', u.deleted_at ? chalk.red('Supprimé') : (u.banned_until ? chalk.red(`Banni jusqu'au ${u.banned_until}`) : chalk.green('Actif'))]
    );
    console.log(table.toString());

    const { action } = await inquirer.prompt([{
      type: 'list', name: 'action', message: 'Action :',
      choices: [
        'Bannir', 'Débannir', 'Avertir', 'Créditer des Pièces', 'Débiter des Pièces',
        'Supprimer le compte (Soft)', 'Supprimer le compte (Hard)', 'Changer abonnement', 'Retour'
      ]
    }]);

    if (action === 'Retour') break;

    if (action === 'Bannir') {
      const { duration } = await inquirer.prompt([{
        type: 'list', name: 'duration', message: 'Durée :',
        choices: [{name: '1 jour', value: 1}, {name: '7 jours', value: 7}, {name: '30 jours', value: 30}, {name: 'Permanent', value: 3650}]
      }]);
      const date = new Date();
      date.setDate(date.getDate() + duration);
      db.prepare("UPDATE users SET banned_until = ? WHERE id = ?").run(date.toISOString(), u.id);
      logAction('ban_user', u.id, 'user', { duration });
      console.log(chalk.green('Utilisateur banni.'));
    } else if (action === 'Débannir') {
      db.prepare("UPDATE users SET banned_until = NULL WHERE id = ?").run(u.id);
      logAction('unban_user', u.id, 'user');
      console.log(chalk.green('Utilisateur débanni.'));
    } else if (action === 'Avertir') {
      db.prepare("INSERT INTO notifications (user_id, type, target_type, target_id) VALUES (?, 'system_warning', 'user', ?)").run(u.id, u.id);
      logAction('warn_user', u.id, 'user');
      console.log(chalk.green('Avertissement envoyé.'));
    } else if (action === 'Créditer des Pièces' || action === 'Débiter des Pièces') {
      const { amount } = await inquirer.prompt([{ type: 'number', name: 'amount', message: 'Montant :' }]);
      if (amount > 0) {
        const factor = action === 'Créditer des Pièces' ? 1 : -1;
        db.prepare("UPDATE users SET pieces = pieces + ? WHERE id = ?").run(amount * factor, u.id);
        logAction(factor > 0 ? 'credit_pieces' : 'debit_pieces', u.id, 'user', { amount });
        console.log(chalk.green('Solde mis à jour.'));
      }
    } else if (action === 'Supprimer le compte (Soft)') {
      const { confirm } = await inquirer.prompt([{ type: 'input', name: 'confirm', message: 'Tapez CONFIRMER pour valider :' }]);
      if (confirm === 'CONFIRMER') {
        db.prepare("UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?").run(u.id);
        logAction('soft_delete_user', u.id, 'user');
        console.log(chalk.green('Compte supprimé (soft).'));
        break; // Leave detail menu
      }
    } else if (action === 'Supprimer le compte (Hard)') {
      const { confirm } = await inquirer.prompt([{ type: 'input', name: 'confirm', message: 'ATTENTION: Tapez CONFIRMER pour supprimer définitivement :' }]);
      if (confirm === 'CONFIRMER') {
        db.prepare("DELETE FROM users WHERE id = ?").run(u.id);
        logAction('hard_delete_user', u.id, 'user');
        console.log(chalk.green('Compte supprimé définitivement.'));
        break; // Leave detail menu
      }
    } else if (action === 'Changer abonnement') {
      const { tier } = await inquirer.prompt([{
        type: 'list', name: 'tier', message: 'Nouveau tier :', choices: ['free', 'premium', 'premium_plus']
      }]);
      db.prepare("UPDATE users SET subscription_tier = ? WHERE id = ?").run(tier, u.id);
      logAction('change_subscription', u.id, 'user', { tier });
      console.log(chalk.green('Abonnement mis à jour.'));
    }
  }
}

async function manageUsers() {
  const { action } = await inquirer.prompt([{
    type: 'list', name: 'action', message: '👥 Gérer les utilisateurs',
    choices: ['Rechercher un utilisateur', 'Voir la liste des derniers inscrits', 'Retour']
  }]);
  if (action === 'Retour') return;

  if (action === 'Rechercher un utilisateur') {
    const { search } = await inquirer.prompt([{ type: 'input', name: 'search', message: 'ID, username ou email Firebase :' }]);
    const users = db.prepare("SELECT * FROM users WHERE id = ? OR username LIKE ? OR firebase_uid = ? LIMIT 10").all(search, `%${search}%`, search);
    if (users.length === 0) { console.log(chalk.red('Aucun utilisateur trouvé.')); return; }
    await selectAndManageUser(users);
  } else if (action === 'Voir la liste des derniers inscrits') {
    const users = db.prepare("SELECT * FROM users ORDER BY id DESC LIMIT 20").all();
    await selectAndManageUser(users);
  }
}
async function selectAndManageCharacter(chars) {
  if (chars.length === 0) return;
  const choices = chars.map(c => ({
    name: `${c.id} | ${c.name} | Likes: ${c.likes || 0} | ${c.is_public ? 'Public' : 'Privé'} | ${c.deleted_at ? chalk.red('(Supprimé)') : ''}`,
    value: c
  }));
  choices.push({ name: 'Retour', value: null });
  const { selected } = await inquirer.prompt([{ type: 'list', name: 'selected', message: 'Sélectionnez un personnage :', choices, pageSize: 15 }]);
  if (!selected) return;
  await charDetailMenu(selected.id);
}

async function charDetailMenu(charId) {
  while (true) {
    const c = db.prepare(`
      SELECT c.*, u.username,
        (SELECT count(*) FROM character_likes WHERE character_id = c.id) as likes_count
      FROM characters c
      LEFT JOIN users u ON c.user_id = u.id
      WHERE c.id = ?
    `).get(charId);
    if (!c) { console.log(chalk.red('Personnage introuvable.')); return; }

    const reportCount = db.prepare("SELECT count(*) as c FROM reports WHERE target_type = 'character' AND target_id = ?").get(c.id).c;

    console.log(chalk.blue('\n=== Détail du personnage ==='));
    const table = new Table({ colWidths: [30, 50] });
    table.push(
      ['ID', c.id],
      ['Nom', c.name],
      ['Créateur', c.username || 'Système'],
      ['Date création', c.created_at || '-'],
      ['Likes', c.likes_count],
      ['Imports', c.imported_count || 0],
      ['Statut', c.deleted_at ? chalk.red('Supprimé') : (c.is_public ? chalk.green('Public') : chalk.yellow('Privé'))],
      ['Signalements', reportCount],
      ['System Prompt', c.system_prompt ? c.system_prompt.substring(0, 45) + '...' : '-']
    );
    console.log(table.toString());

    const { action } = await inquirer.prompt([{
      type: 'list', name: 'action', message: 'Action :',
      choices: [
        'Dépublier', 'Supprimer (Soft)', 'Supprimer (Hard)', 'Signaler à l\'utilisateur', 'Retour'
      ]
    }]);
    if (action === 'Retour') break;

    if (action === 'Dépublier') {
      db.prepare("UPDATE characters SET is_public = 0 WHERE id = ?").run(c.id);
      logAction('unpublish_character', c.id, 'character');
      console.log(chalk.green('Personnage dépublié.'));
    } else if (action === 'Supprimer (Soft)') {
      const { confirm } = await inquirer.prompt([{ type: 'input', name: 'confirm', message: 'Tapez CONFIRMER :' }]);
      if (confirm === 'CONFIRMER') {
        db.prepare("UPDATE characters SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?").run(c.id);
        logAction('soft_delete_character', c.id, 'character');
        console.log(chalk.green('Personnage supprimé (soft).'));
        break;
      }
    } else if (action === 'Supprimer (Hard)') {
      const { confirm } = await inquirer.prompt([{ type: 'input', name: 'confirm', message: 'ATTENTION: Tapez CONFIRMER :' }]);
      if (confirm === 'CONFIRMER') {
        db.prepare("DELETE FROM characters WHERE id = ?").run(c.id);
        logAction('hard_delete_character', c.id, 'character');
        console.log(chalk.green('Personnage supprimé définitivement.'));
        break;
      }
    } else if (action === 'Signaler à l\'utilisateur') {
      db.prepare("INSERT INTO notifications (user_id, type, target_type, target_id) VALUES (?, 'character_warning', 'character', ?)").run(c.user_id, c.id);
      logAction('warn_character', c.id, 'character');
      console.log(chalk.green('Avertissement envoyé au créateur.'));
    }
  }
}

async function manageCharacters() {
  const { action } = await inquirer.prompt([{
    type: 'list', name: 'action', message: '🤖 Gérer les personnages',
    choices: ['Rechercher un personnage', 'Voir la liste des personnages publics', 'Retour']
  }]);
  if (action === 'Retour') return;

  if (action === 'Rechercher un personnage') {
    const { search } = await inquirer.prompt([{ type: 'input', name: 'search', message: 'ID ou nom :' }]);
    const chars = db.prepare("SELECT * FROM characters WHERE id = ? OR name LIKE ? LIMIT 10").all(search, `%${search}%`);
    if (chars.length === 0) { console.log(chalk.red('Aucun personnage trouvé.')); return; }
    await selectAndManageCharacter(chars);
  } else if (action === 'Voir la liste des personnages publics') {
    const chars = db.prepare("SELECT c.*, (SELECT count(*) FROM character_likes WHERE character_id = c.id) as likes FROM characters c WHERE is_public = 1 AND deleted_at IS NULL ORDER BY likes DESC LIMIT 20").all();
    await selectAndManageCharacter(chars);
  }
}
async function selectAndManageStory(stories) {
  if (stories.length === 0) return;
  const choices = stories.map(s => ({
    name: `${s.id} | ${s.title} | ${s.genre} | ${s.is_published ? 'Publié' : 'Privé'} | ${s.deleted_at ? chalk.red('(Supprimé)') : ''}`,
    value: s
  }));
  choices.push({ name: 'Retour', value: null });
  const { selected } = await inquirer.prompt([{ type: 'list', name: 'selected', message: 'Sélectionnez une histoire :', choices, pageSize: 15 }]);
  if (!selected) return;
  await storyDetailMenu(selected.id);
}

async function storyDetailMenu(storyId) {
  while (true) {
    const s = db.prepare(`
      SELECT s.*, u.username
      FROM stories s
      LEFT JOIN users u ON s.user_id = u.id
      WHERE s.id = ?
    `).get(storyId);
    if (!s) { console.log(chalk.red('Histoire introuvable.')); return; }

    const nodeCount = db.prepare("SELECT count(*) as c FROM story_nodes WHERE story_id = ?").get(s.id).c;
    const playCount = db.prepare("SELECT count(*) as c FROM story_sessions WHERE story_id = ?").get(s.id).c;

    console.log(chalk.blue('\n=== Détail de l\'histoire ==='));
    const table = new Table({ colWidths: [30, 50] });
    table.push(
      ['ID', s.id],
      ['Titre', s.title],
      ['Créateur', s.username || 'Inconnu'],
      ['Genre', s.genre || '-'],
      ['Noeuds', nodeCount],
      ['Parties jouées', playCount],
      ['Statut', s.deleted_at ? chalk.red('Supprimé') : (s.is_published ? chalk.green('Publié') : chalk.yellow('Privé'))]
    );
    console.log(table.toString());

    const { action } = await inquirer.prompt([{
      type: 'list', name: 'action', message: 'Action :',
      choices: ['Dépublier', 'Supprimer (Soft)', 'Supprimer (Hard)', 'Retour']
    }]);
    if (action === 'Retour') break;

    if (action === 'Dépublier') {
      db.prepare("UPDATE stories SET is_published = 0 WHERE id = ?").run(s.id);
      logAction('unpublish_story', s.id, 'story');
      console.log(chalk.green('Histoire dépubliée.'));
    } else if (action === 'Supprimer (Soft)') {
      const { confirm } = await inquirer.prompt([{ type: 'input', name: 'confirm', message: 'Tapez CONFIRMER :' }]);
      if (confirm === 'CONFIRMER') {
        db.prepare("UPDATE stories SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?").run(s.id);
        logAction('soft_delete_story', s.id, 'story');
        console.log(chalk.green('Histoire supprimée (soft).'));
        break;
      }
    } else if (action === 'Supprimer (Hard)') {
      const { confirm } = await inquirer.prompt([{ type: 'input', name: 'confirm', message: 'ATTENTION: Tapez CONFIRMER :' }]);
      if (confirm === 'CONFIRMER') {
        db.prepare("DELETE FROM stories WHERE id = ?").run(s.id);
        logAction('hard_delete_story', s.id, 'story');
        console.log(chalk.green('Histoire supprimée définitivement.'));
        break;
      }
    }
  }
}

async function manageStories() {
  const { action } = await inquirer.prompt([{
    type: 'list', name: 'action', message: '📖 Gérer les histoires',
    choices: ['Rechercher une histoire', 'Voir la liste des histoires publiées', 'Retour']
  }]);
  if (action === 'Retour') return;

  if (action === 'Rechercher une histoire') {
    const { search } = await inquirer.prompt([{ type: 'input', name: 'search', message: 'ID ou titre :' }]);
    const stories = db.prepare("SELECT * FROM stories WHERE id = ? OR title LIKE ? LIMIT 10").all(search, `%${search}%`);
    if (stories.length === 0) { console.log(chalk.red('Aucune histoire trouvée.')); return; }
    await selectAndManageStory(stories);
  } else if (action === 'Voir la liste des histoires publiées') {
    const stories = db.prepare("SELECT * FROM stories WHERE is_published = 1 AND deleted_at IS NULL ORDER BY created_at DESC LIMIT 20").all();
    await selectAndManageStory(stories);
  }
}
async function manageReports() {
  while (true) {
    const { action } = await inquirer.prompt([{
      type: 'list', name: 'action', message: '🚨 Modération (signalements)',
      choices: ['Voir les signalements en attente', 'Voir l\'historique', 'Retour']
    }]);
    if (action === 'Retour') return;

    if (action === 'Voir les signalements en attente') {
      const reports = db.prepare(`
        SELECT id, target_type, target_id, reason, created_at,
          (SELECT count(*) FROM reports r2 WHERE r2.target_type = reports.target_type AND r2.target_id = reports.target_id) as total_reports
        FROM reports
        WHERE status = 'pending'
        ORDER BY created_at ASC LIMIT 50
      `).all();

      if (reports.length === 0) {
        console.log(chalk.green('Aucun signalement en attente.'));
        continue;
      }

      const choices = reports.map(r => ({
        name: `[${r.id}] ${r.target_type} #${r.target_id} | ${r.reason} | Cumul: ${r.total_reports} | ${r.created_at}`,
        value: r
      }));
      choices.push({ name: 'Retour', value: null });

      const { selected } = await inquirer.prompt([{ type: 'list', name: 'selected', message: 'Sélectionnez un signalement :', choices, pageSize: 15 }]);
      if (selected) {
        await reportDetailMenu(selected.id);
      }
    } else if (action === 'Voir l\'historique') {
      const reports = db.prepare("SELECT * FROM reports WHERE status != 'pending' ORDER BY created_at DESC LIMIT 50").all();
      const table = new Table({ head: ['ID', 'Type', 'Target', 'Status', 'Date'], colWidths: [10, 15, 10, 15, 25] });
      reports.forEach(r => table.push([r.id, r.target_type, r.target_id, r.status, r.created_at]));
      console.log(table.toString());
    }
  }
}

async function reportDetailMenu(reportId) {
  while (true) {
    const r = db.prepare("SELECT * FROM reports WHERE id = ?").get(reportId);
    if (!r) return;

    console.log(chalk.blue('\n=== Détail du signalement ==='));
    const table = new Table({ colWidths: [30, 50] });
    table.push(
      ['ID', r.id],
      ['Date', r.created_at],
      ['Type de cible', r.target_type],
      ['ID de la cible', r.target_id],
      ['Raison', r.reason],
      ['Statut', r.status]
    );

    let contentPreview = 'Inconnu';
    let authorId = null;
    if (r.target_type === 'user') {
      const u = db.prepare("SELECT username, id FROM users WHERE id = ?").get(r.target_id);
      if (u) { contentPreview = `Username: ${u.username}`; authorId = u.id; }
    } else if (r.target_type === 'character') {
      const c = db.prepare("SELECT name, system_prompt, user_id FROM characters WHERE id = ?").get(r.target_id);
      if (c) { contentPreview = `Nom: ${c.name}\nPrompt: ${c.system_prompt.substring(0, 50)}...`; authorId = c.user_id; }
    } else if (r.target_type === 'story') {
      const s = db.prepare("SELECT title, user_id FROM stories WHERE id = ?").get(r.target_id);
      if (s) { contentPreview = `Titre: ${s.title}`; authorId = s.user_id; }
    } else if (r.target_type === 'post') {
      const p = db.prepare("SELECT content, user_id FROM posts WHERE id = ?").get(r.target_id);
      if (p) { contentPreview = `Contenu: ${p.content}`; authorId = p.user_id; }
    } else if (r.target_type === 'comment') {
      const p = db.prepare("SELECT content, user_id FROM post_comments WHERE id = ?").get(r.target_id);
      if (p) { contentPreview = `Contenu: ${p.content}`; authorId = p.user_id; }
    }
    table.push(['Aperçu du contenu', contentPreview]);
    table.push(['Auteur du contenu', authorId || 'Inconnu']);

    console.log(table.toString());

    const { action } = await inquirer.prompt([{
      type: 'list', name: 'action', message: 'Action :',
      choices: [
        'Ignorer (Dismiss)', 'Supprimer le contenu (Soft Delete)', 'Avertir l\'auteur', 'Bannir l\'auteur', 'Retour'
      ]
    }]);

    if (action === 'Retour') break;

    if (action === 'Ignorer (Dismiss)') {
      db.prepare("UPDATE reports SET status = 'dismissed' WHERE id = ?").run(r.id);
      logAction('dismiss_report', r.id, 'report');
      console.log(chalk.green('Signalement ignoré.'));
      break;
    } else if (action === 'Supprimer le contenu (Soft Delete)') {
      const { confirm } = await inquirer.prompt([{ type: 'input', name: 'confirm', message: 'Tapez CONFIRMER :' }]);
      if (confirm === 'CONFIRMER') {
        if (['user', 'character', 'story', 'post', 'post_comments'].includes(r.target_type)) {
          let table = r.target_type;
          if (table === 'comment') table = 'post_comments';
          db.prepare(`UPDATE ${table} SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?`).run(r.target_id);
        }
        db.prepare("UPDATE reports SET status = 'resolved' WHERE target_type = ? AND target_id = ?").run(r.target_type, r.target_id);
        logAction('delete_reported_content', r.target_id, r.target_type);
        console.log(chalk.green('Contenu supprimé.'));
        break;
      }
    } else if (action === 'Avertir l\'auteur') {
      if (authorId) {
        db.prepare("INSERT INTO notifications (user_id, type, target_type, target_id) VALUES (?, 'system_warning', ?, ?)").run(authorId, r.target_type, r.target_id);
        db.prepare("UPDATE reports SET status = 'resolved' WHERE id = ?").run(r.id);
        logAction('warn_reported_user', authorId, 'user');
        console.log(chalk.green('Avertissement envoyé.'));
        break;
      } else {
        console.log(chalk.red('Auteur inconnu.'));
      }
    } else if (action === 'Bannir l\'auteur') {
      if (authorId) {
        const { duration } = await inquirer.prompt([{
          type: 'list', name: 'duration', message: 'Durée :',
          choices: [{name: '1 jour', value: 1}, {name: '7 jours', value: 7}, {name: '30 jours', value: 30}, {name: 'Permanent', value: 3650}]
        }]);
        const date = new Date();
        date.setDate(date.getDate() + duration);
        db.prepare("UPDATE users SET banned_until = ? WHERE id = ?").run(date.toISOString(), authorId);
        db.prepare("UPDATE reports SET status = 'resolved' WHERE id = ?").run(r.id);
        logAction('ban_reported_user', authorId, 'user', { duration });
        console.log(chalk.green('Auteur banni.'));
        break;
      } else {
        console.log(chalk.red('Auteur inconnu.'));
      }
    }
  }
}
async function manageEconomy() {
  console.log(chalk.blue('\n=== 💰 Économie ==='));

  const totalCirculating = db.prepare('SELECT sum(pieces) as s FROM users WHERE deleted_at IS NULL').get().s || 0;

  let totalPurchased = 0;
  try {
    totalPurchased = db.prepare("SELECT sum(amount) as s FROM piece_transactions WHERE type IN ('purchase', 'subscription')").get().s || 0;
  } catch (e) {}

  let totalFree = 0;
  try {
    totalFree = db.prepare("SELECT sum(amount) as s FROM piece_transactions WHERE type IN ('daily_reward', 'quest_reward', 'ad_reward')").get().s || 0;
  } catch (e) {}

  const tableStats = new Table({ colWidths: [40, 20] });
  tableStats.push(
    ['Total Pièces en circulation', totalCirculating],
    ['Total Pièces achetées', totalPurchased],
    ['Total Pièces gagnées (récompenses)', totalFree]
  );
  console.log(tableStats.toString());

  console.log(chalk.cyan('\nTop 10 utilisateurs (Pièces) :'));
  const topUsers = db.prepare("SELECT id, username, pieces FROM users WHERE deleted_at IS NULL ORDER BY pieces DESC LIMIT 10").all();
  const tableUsers = new Table({ head: ['ID', 'Username', 'Pièces'], colWidths: [10, 30, 20] });
  topUsers.forEach(u => tableUsers.push([u.id, u.username, u.pieces]));
  console.log(tableUsers.toString());

  console.log(chalk.cyan('\nTop 10 créateurs (Revenus) :'));
  let topCreators = [];
  try {
    topCreators = db.prepare(`
      SELECT user_id, sum(amount) as total_earnings
      FROM creator_transactions
      WHERE type = 'earn'
      GROUP BY user_id
      ORDER BY total_earnings DESC LIMIT 10
    `).all();
  } catch (e) {}

  const tableCreators = new Table({ head: ['ID', 'Username', 'Revenus (Pièces)'], colWidths: [10, 30, 20] });
  topCreators.forEach(c => {
    const u = db.prepare("SELECT username FROM users WHERE id = ?").get(c.user_id);
    tableCreators.push([c.user_id, u ? u.username : 'Inconnu', c.total_earnings]);
  });
  console.log(tableCreators.toString());

  console.log(chalk.cyan('\nDernières 50 transactions :'));
  let recentTransactions = [];
  try {
    recentTransactions = db.prepare("SELECT * FROM piece_transactions ORDER BY created_at DESC LIMIT 50").all();
  } catch (e) {}
  const tableTx = new Table({ head: ['Date', 'User ID', 'Type', 'Montant'], colWidths: [25, 10, 30, 15] });
  recentTransactions.forEach(t => tableTx.push([t.created_at, t.user_id, t.type, t.amount]));
  console.log(tableTx.toString());

  await inquirer.prompt([{ type: 'input', name: 'continue', message: 'Appuyez sur Entrée pour continuer...' }]);
}

async function start() {
  await login();
  await mainMenu();
}

start();
