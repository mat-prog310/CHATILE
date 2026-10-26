import dotenv from 'dotenv';
dotenv.config();

async function test() {
  if (!process.env.MISTRAL_API_KEY) {
    console.error('❌ MISTRAL_API_KEY manquante');
    process.exit(1);
  }
  
  console.log('Test de la clé Mistral...');
  const response = await fetch('https://api.mistral.ai/v1/chat/completions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${process.env.MISTRAL_API_KEY}`
    },
    body: JSON.stringify({
      model: 'mistral-small-2603',
      messages: [{ role: 'user', content: 'Réponds juste "OK"' }],
      max_tokens: 10
    })
  });
  
  if (!response.ok) {
    console.error('❌ Erreur:', response.status, await response.text());
    process.exit(1);
  }
  
  const data = await response.json();
  console.log('✅ Clé valide. Réponse:', data.choices[0].message.content);
}

test();
