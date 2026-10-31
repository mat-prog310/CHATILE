import 'dotenv/config';

async function testOpenAI() {
  const messages = [
    { role: 'system', content: 'Tu es un assistant.' },
    { role: 'user', content: 'Bonjour' }
  ];

  try {
    const res = await fetch('https://api.openai.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${process.env.OPENAI_API_KEY}`
      },
      body: JSON.stringify({
        model: 'gpt-4o-mini',
        messages,
        max_tokens: 600,
        temperature: 0.85,
        presence_penalty: 0.4,
        frequency_penalty: 0.4,
      })
    });

    console.log('Status:', res.status);
    const text = await res.text();
    console.log('Body:', text);
  } catch (err) {
    console.error('Error:', err);
  }
}

testOpenAI();
