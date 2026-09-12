// One-off utility to bootstrap or promote a super admin account directly via
// the Admin SDK, bypassing the app's normal signup/approval flow entirely.
//
// Usage: node create-admin.js <email> <password> [displayName]

const path = require('path');
const fs = require('fs');
const admin = require('firebase-admin');

const serviceAccountPath = path.join(__dirname, 'serviceAccountKey.json');
if (!fs.existsSync(serviceAccountPath)) {
  console.error('\nMissing admin-panel/serviceAccountKey.json. See README/instructions to generate one.\n');
  process.exit(1);
}

const serviceAccount = require(serviceAccountPath);
admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });

const [, , email, password, name] = process.argv;
if (!email || !password) {
  console.error('Usage: node create-admin.js <email> <password> [displayName]');
  process.exit(1);
}

(async () => {
  const db = admin.firestore();
  let userRecord;

  try {
    userRecord = await admin.auth().getUserByEmail(email);
    console.log(`User ${email} already exists (uid=${userRecord.uid}). Updating password...`);
    await admin.auth().updateUser(userRecord.uid, {
      password,
      displayName: name || userRecord.displayName || email.split('@')[0]
    });
  } catch (err) {
    if (err.code === 'auth/user-not-found') {
      userRecord = await admin.auth().createUser({
        email,
        password,
        displayName: name || email.split('@')[0]
      });
      console.log(`Created new user ${email} (uid=${userRecord.uid}).`);
    } else {
      throw err;
    }
  }

  await db.collection('users').doc(userRecord.uid).set(
    {
      name: name || userRecord.displayName || email.split('@')[0],
      email,
      role: 'ADMIN'
    },
    { merge: true }
  );

  console.log(`\n✔ ${email} is now an ADMIN. uid=${userRecord.uid}\n`);
  process.exit(0);
})().catch((err) => {
  console.error('Failed:', err.message);
  process.exit(1);
});
