import 'dotenv/config';

async function testApiChat() {
  try {
    // First let's get a device id or user token, or test callOpenAI directly
    const res = await fetch('http://localhost:3000/api/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-device-id': 'test_device_123'
      },
      body: JSON.stringify({
        message: 'Bonjour Léon',
        characterId: null,
        mode: 'normal'
      })
    });
    console.log('Status:', res.status);
    const data = await res.json();
    console.log('Body:', data);
  } catch (err) {
    console.error('Fetch error:', err);
  }
}

testApiChat();
