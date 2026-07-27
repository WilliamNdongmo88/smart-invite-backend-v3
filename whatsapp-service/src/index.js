require('dotenv').config();
const express = require('express');
const app = express();

app.use(express.json());

app.use('/api', require('./routes/messages'));

app.get('/health', (req, res) => res.json({ status: 'ok' }));

const PORT = process.env.PORT || 3001;
app.listen(PORT, () => {
    console.log(`[Server] WhatsApp service démarré sur le port ${PORT}`);
});
