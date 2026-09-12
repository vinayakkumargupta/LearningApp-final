const path = require('path');
const fs = require('fs');
const express = require('express');
const multer = require('multer');
const admin = require('firebase-admin');

const serviceAccountPath = path.join(__dirname, 'serviceAccountKey.json');
if (!fs.existsSync(serviceAccountPath)) {
  console.error(
    '\nMissing admin-panel/serviceAccountKey.json.\n' +
    'Get it from Firebase Console -> Project Settings (gear icon) -> Service accounts -> Generate new private key,\n' +
    'then save the downloaded file as admin-panel/serviceAccountKey.json\n'
  );
  process.exit(1);
}

const googleServicesPath = path.join(__dirname, '..', 'app', 'google-services.json');
const storageBucket = JSON.parse(fs.readFileSync(googleServicesPath, 'utf8'))
  .project_info.storage_bucket;

const serviceAccount = require(serviceAccountPath);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  storageBucket
});

const db = admin.firestore();
const bucket = admin.storage().bucket();

const uploadsDir = path.join(__dirname, 'uploads');
fs.mkdirSync(uploadsDir, { recursive: true });
const upload = multer({ dest: uploadsDir });

const app = express();
app.use(express.json({ limit: '5mb' }));
app.use(express.static(path.join(__dirname, 'public')));

// ---------- Exams ----------

app.get('/api/exams', async (req, res) => {
  try {
    const snapshot = await db.collection('test_series').get();
    const exams = await Promise.all(snapshot.docs.map(async (doc) => {
      const qSnap = await doc.ref.collection('questions').get();
      return { id: doc.id, ...doc.data(), questionCount: qSnap.size };
    }));
    res.json(exams);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

async function saveExam(payload) {
  const {
    id, title, subject, durationMinutes, totalMarks,
    passingMarks, priceInInr, examCategory, questions
  } = payload;

  if (!id || !title || !Array.isArray(questions) || questions.length === 0) {
    throw new Error('id, title, and at least one question are required');
  }
  for (const q of questions) {
    if (!q.questionText || !Array.isArray(q.options) || q.options.length < 2) {
      throw new Error('Every question needs text and at least 2 options');
    }
    if (
      q.correctOptionIndex === undefined ||
      Number(q.correctOptionIndex) < 0 ||
      Number(q.correctOptionIndex) >= q.options.length
    ) {
      throw new Error('correctOptionIndex must point at a valid option');
    }
  }

  const examRef = db.collection('test_series').doc(id);
  await examRef.set({
    title,
    subject: subject || 'General',
    totalQuestions: questions.length,
    durationMinutes: Number(durationMinutes) || 60,
    totalMarks: Number(totalMarks) || questions.length * 4,
    passingMarks: Number(passingMarks) || 0,
    isCompleted: false,
    isLocked: Number(priceInInr) > 0,
    priceInInr: Number(priceInInr) || 0,
    examCategory: examCategory || 'MCQ Mock Test'
  });

  // Wipe any existing questions before writing the new set (keeps edits clean)
  const existing = await examRef.collection('questions').get();
  const clearBatch = db.batch();
  existing.docs.forEach((d) => clearBatch.delete(d.ref));
  await clearBatch.commit();

  const batch = db.batch();
  questions.forEach((q, idx) => {
    const qRef = examRef.collection('questions').doc(`q${idx + 1}`);
    batch.set(qRef, {
      id: idx + 1,
      questionText: q.questionText,
      options: q.options,
      correctOptionIndex: Number(q.correctOptionIndex),
      explanation: q.explanation || '',
      subject: q.subject || subject || 'General'
    });
  });
  await batch.commit();

  return { id, questionCount: questions.length };
}

app.post('/api/exams', async (req, res) => {
  try {
    const result = await saveExam(req.body);
    res.json({ ok: true, ...result });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Accepts either a single exam object or an array of exam objects.
// Each exam is saved independently so one bad entry doesn't block the rest.
app.post('/api/exams/bulk', async (req, res) => {
  const exams = Array.isArray(req.body) ? req.body : [req.body];
  const results = [];
  for (const examPayload of exams) {
    try {
      const result = await saveExam(examPayload);
      results.push({ ok: true, ...result });
    } catch (err) {
      results.push({ ok: false, id: examPayload && examPayload.id, error: err.message });
    }
  }
  res.json({ results });
});

app.delete('/api/exams/:id', async (req, res) => {
  try {
    const examRef = db.collection('test_series').doc(req.params.id);
    const qSnap = await examRef.collection('questions').get();
    const batch = db.batch();
    qSnap.docs.forEach((d) => batch.delete(d.ref));
    batch.delete(examRef);
    await batch.commit();
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ---------- Materials ----------

app.get('/api/materials', async (req, res) => {
  try {
    const snapshot = await db.collection('materials').get();
    const materials = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
    res.json(materials);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/materials', upload.single('file'), async (req, res) => {
  try {
    const {
      id, title, subject, description, category, materialType,
      priceInInr, videoUrl, videoDuration, pagesCount
    } = req.body;

    if (!id || !title) {
      if (req.file) fs.unlinkSync(req.file.path);
      return res.status(400).json({ error: 'id and title are required' });
    }

    let pdfUrl = '';
    let fileSizeMb = '';

    if (req.file) {
      const destination = `materials/${id}_${req.file.originalname}`;
      await bucket.upload(req.file.path, {
        destination,
        public: true,
        metadata: { contentType: req.file.mimetype }
      });
      fs.unlinkSync(req.file.path);
      pdfUrl = bucket.file(destination).publicUrl();
      fileSizeMb = `${(req.file.size / (1024 * 1024)).toFixed(1)} MB`;
    }

    await db.collection('materials').doc(id).set({
      title,
      subject: subject || 'General',
      pagesCount: Number(pagesCount) || 0,
      fileSizeMb: fileSizeMb || '',
      pdfUrl,
      videoUrl: videoUrl || null,
      videoDuration: videoDuration || null,
      description: description || '',
      category: category || 'Notes',
      materialType: materialType === 'VIDEO' ? 'VIDEO' : 'PDF',
      isLocked: Number(priceInInr) > 0,
      priceInInr: Number(priceInInr) || 0
    });

    res.json({ ok: true, id, pdfUrl });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/materials/:id', async (req, res) => {
  try {
    await db.collection('materials').doc(req.params.id).delete();
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ---------- Users / Admin approvals ----------

const VALID_ROLES = ['MEMBER', 'PENDING_ADMIN', 'ADMIN'];

app.get('/api/users', async (req, res) => {
  try {
    const snapshot = await db.collection('users').get();
    const users = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
    res.json(users);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/users/:uid/role', async (req, res) => {
  try {
    const { role } = req.body;
    if (!VALID_ROLES.includes(role)) {
      return res.status(400).json({ error: `role must be one of ${VALID_ROLES.join(', ')}` });
    }
    await db.collection('users').doc(req.params.uid).update({ role });
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

const PORT = process.env.PORT || 5050;
app.listen(PORT, () => {
  console.log(`Admin panel running at http://localhost:${PORT}`);
});
