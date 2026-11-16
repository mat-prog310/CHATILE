const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '.env') });

let currentApiUrl = process.env.API_BASE_URL || 'https://chatile.onrender.com';
let currentAdminPassword = process.env.ADMIN_PASSWORD || 'admin';

async function apiCall(endpoint, method = 'GET', body = null, retries = 3) {
  for (let attempt = 1; attempt <= retries; attempt++) {
    try {
      const options = {
        method,
        headers: {
          'Content-Type': 'application/json',
          'x-admin-secret': currentAdminPassword
        }
      };
      if (body) {
        options.body = JSON.stringify(body);
      }
      const response = await fetch(`${currentApiUrl}${endpoint}`, options);
      const data = await response.json();
      if (!response.ok) {
        return { success: false, error: data.error || `Erreur HTTP ${response.status}` };
      }
      return { success: true, ...data };
    } catch (err) {
      if (attempt === retries) {
        console.error(`API Call failed (${currentApiUrl}${endpoint}):`, err);
        return { success: false, error: `Connexion refusée à ${currentApiUrl}. Vérifiez l'URL du serveur.` };
      }
      await new Promise(resolve => setTimeout(resolve, 3000));
    }
  }
}

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false
    },
    autoHideMenuBar: true,
    title: 'PiecesIA Admin Panel'
  });

  mainWindow.loadFile('index.html');
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

// --- IPC Handlers ---

ipcMain.handle('admin:login', async (event, { apiUrl, password }) => {
  if (apiUrl) currentApiUrl = apiUrl.trim().replace(/\/$/, '');
  if (password) currentAdminPassword = password;

  const res = await apiCall('/api/admin/stats');
  if (res.success) {
    return { success: true };
  }
  return { success: false, error: res.error || 'Authentification ou connexion échouée.' };
});

ipcMain.handle('admin:get-stats', async () => {
  const res = await apiCall('/api/admin/stats');
  if (!res.success) return res;
  return {
    success: true,
    stats: {
      totalUsers: res.totalUsers || 0,
      activeUsers: res.activeUsers || 0,
      totalChars: res.totalCharacters || 0,
      totalStories: res.totalPosts || 0,
      totalPieces: res.piecesDistributed || 0,
      premiumUsers: res.premiumUsers || 0
    }
  };
});

ipcMain.handle('admin:get-users', async (event, { page = 1, limit = 15, search = '', sortBy = 'id', sortOrder = 'DESC' }) => {
  const queryParams = new URLSearchParams({ page, limit, search, sortBy, sortOrder });
  return await apiCall(`/api/admin/users-list?${queryParams.toString()}`);
});

ipcMain.handle('admin:get-user-detail', async (event, userId) => {
  return await apiCall(`/api/admin/users/${userId}/detail`);
});

ipcMain.handle('admin:ban-user', async (event, { userId, days, reason }) => {
  let duration = '7d';
  if (days === 1) duration = '1d';
  else if (days === 30) duration = '30d';
  else if (days >= 365) duration = 'permanent';
  else if (days === 7) duration = '7d';
  return await apiCall(`/api/admin/users/${userId}/ban`, 'POST', { duration, reason });
});

ipcMain.handle('admin:unban-user', async (event, { userId, reason }) => {
  return await apiCall(`/api/admin/users/${userId}/unban`, 'POST', { reason });
});

ipcMain.handle('admin:adjust-pieces', async (event, { userId, amount, reason }) => {
  const res = await apiCall(`/api/admin/users/${userId}/pieces`, 'POST', { amount, reason });
  if (!res.success) return res;
  return { success: true, newPieces: res.newPieces };
});

ipcMain.handle('admin:update-user', async (event, { userId, username, subscriptionTier }) => {
  return await apiCall(`/api/admin/users/${userId}`, 'PUT', { username, subscriptionTier });
});

ipcMain.handle('admin:modify-character', async (event, { charId, name, systemPrompt, isPublic }) => {
  return await apiCall(`/api/admin/characters/${charId}`, 'PUT', { name, systemPrompt, isPublic });
});

ipcMain.handle('admin:delete-character', async (event, charId) => {
  return await apiCall(`/api/admin/characters/${charId}`, 'DELETE');
});

ipcMain.handle('admin:get-audit-logs', async () => {
  return await apiCall('/api/admin/logs');
});
