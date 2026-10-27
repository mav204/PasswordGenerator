# Password Manager (Java)

Un password manager local, scris de la zero în Java 21, ca proiect de învățare pentru criptografie aplicată.
Parolele sunt păstrate într-un singur fișier criptat cu **AES-256-GCM**, deblocat cu o parolă master
prin **PBKDF2-HMAC-SHA256**. Nimic nu ajunge pe disc în clar, nici măcar numele serviciilor.

## Funcționalități

- creare și deschidere de seif cu parolă master
- adăugare, listare, căutare și ștergere de intrări (serviciu, utilizator, parolă, notițe)
- generator de parole (`SecureRandom`, 16 caractere, ~99,7 biți de entropie afișați la generare)
- parola căutată sau generată merge direct în **clipboard** și se șterge automat după **20 de secunde**
- schimbarea parolei master, cu verificarea parolei actuale și confirmarea celei noi
- detectarea oricărei modificări a fișierului (tag GCM + header autentificat)

## Cum funcționează criptarea

```
parola master + salt (16 B) ──PBKDF2-HMAC-SHA256, 600.000 iterații──► KEK (32 B)

DEK (32 B, aleator, generat o singură dată)
   ├── încuiat cu KEK:   AES-256-GCM(KEK, DEK, AAD = header)       → wrappedDek
   └── cheia datelor:    AES-256-GCM(DEK, intrări JSON, AAD = header) → entries
```

- **KEK** (Key Encryption Key) nu e salvat nicăieri: se recalculează din parolă la fiecare deschidere.
- **DEK** (Data Encryption Key) e salvat doar încuiat cu KEK. Schimbarea parolei master re-împachetează
  același DEK cu un KEK nou (salt nou), fără să se piardă intrările.
- **Header-ul** (versiune, algoritm KDF, iterații, salt) e în clar, pentru că e necesar înainte de decriptare,
  dar e trecut ca **AAD** în ambele criptări. Orice modificare a lui e detectată.
- O parolă greșită produce un KEK greșit, deci tag-ul din `wrappedDek` nu se mai potrivește
  (`AEADBadTagException`). Tag-ul servește și ca verificare a parolei, fără să existe un hash al ei salvat.

### Parametri

| Element | Valoare |
|---|---|
| Derivarea cheii | PBKDF2-HMAC-SHA256, 600.000 iterații, cheie de 256 biți |
| Salt | 16 bytes din `SecureRandom`, nou la crearea seifului și la schimbarea parolei |
| Criptare | AES-256-GCM (`AES/GCM/NoPadding`) |
| Nonce | 12 bytes din `SecureRandom`, nou la **fiecare** criptare |
| Tag | 128 biți |
| Generator de parole | `SecureRandom.nextInt` (fără modulo bias), alfabet de 75 de caractere |

### Formatul fișierului

```json
{
  "header":     { "version": 1, "kdf": "PBKDF2WithHmacSHA256", "iterations": 600000, "salt": "..." },
  "wrappedDek": { "nonce": "...", "cipherText": "..." },
  "entries":    { "nonce": "...", "cipherText": "..." }
}
```

Toate valorile binare sunt în Base64. Toate intrările sunt criptate **împreună**, ca un singur bloc,
deci din fișier nu se poate afla nici numărul de intrări, nici numele serviciilor.

## Rulare

Cerințe: Java 21+ și Maven.

```bash
mvn -q compile exec:java -Dexec.mainClass=ro.mateiavram.pm.Main
```

Seiful este creat ca `test-vault.json` în folderul din care rulezi comanda.

```
=== Seif deschis ===
1. Adauga intrare
2. Listeaza intrari
3. Cauta dupa serviciu
4. Sterge intrare
5. Inchide seiful
6. Schimba parola master
0. Inchide si iesi
```

La „Adaugă intrare”, apăsând Enter la parolă se generează una nouă, copiată direct în clipboard.

## Teste

```bash
mvn test
```

16 teste JUnit 5 pentru `CryptoService` și pentru fluxul KEK/DEK, inclusiv cazuri de atac care **trebuie să eșueze**:

| Test | Ce demonstrează |
|---|---|
| `decryptWithWrongKey` | o cheie greșită nu decriptează nimic |
| `modifiedCipherText` | un singur bit modificat în ciphertext e detectat |
| `decryptWithDifferentAadThrows` | un header modificat e detectat |
| `incorrectPasswordClosedSeif` | o parolă master greșită nu despachetează DEK-ul |
| `differentCipherTextOnSameEncryption` | aceleași date criptate de două ori dau rezultate diferite (nonce nou) |
| `kekDeterminism` | aceeași parolă și același salt dau același KEK, deci seiful se poate redeschide |
| `passwordChange` | după re-împachetarea DEK-ului cu o parolă nouă, datele vechi rămân accesibile |

## Threat model

**Ce protejăm:** parolele, utilizatorii și notițele din seif.

**Atacatorul considerat:** cineva care obține o copie a fișierului seifului (laptop furat, backup, cloud)
și îl poate citi și modifica după bunul plac, dar nu cunoaște parola master.

| Amenințare (STRIDE) | Exemplu | Protecție |
|---|---|---|
| **Information disclosure** | citirea parolelor din fișier | AES-256-GCM; DEK-ul e salvat doar încuiat |
| **Tampering** | modificarea unui byte din fișier sau din header | tag GCM + header ca AAD → `AEADBadTagException` |
| **Spoofing** | un fișier de seif fabricat sau dintr-o versiune necunoscută | validarea header-ului (versiune, KDF, iterații, salt) |
| **Brute force offline** | ghicirea parolei master | PBKDF2 cu 600.000 iterații și salt unic → ~0,2 s pe încercare pe un laptop |
| **Denial of service** | programul se oprește în timpul salvării | scriere atomică: fișier `.tmp` + `ATOMIC_MOVE` |

Secretele sunt ținute cât mai puțin în memorie: parola master e `char[]` și se șterge cu `Arrays.fill`,
KEK-ul se șterge imediat după despachetarea DEK-ului, iar DEK-ul la închiderea seifului.
Parolele intrărilor nu sunt afișate pe ecran și nu apar în `toString()`.

### Ce NU protejează

- **O parolă master slabă.** Iterațiile încetinesc atacul, dar o parolă din dicționar (de exemplu `parola123`)
  poate fi spartă cu un GPU în câteva minute. Siguranța depinde de o parolă master lungă.
- **Memoria în timp ce seiful e deschis.** Intrările decriptate sunt în RAM; parolele sunt `String`,
  imuabile, deci nu pot fi șterse explicit. Un atacator cu acces la memoria procesului le poate citi.
- **Clipboard-ul.** Alte programe pot citi clipboard-ul în cele 20 de secunde; managerele de istoric al
  clipboard-ului și Universal Clipboard (iCloud) pot păstra copii pe care programul nu le poate șterge.
  Dacă programul e oprit forțat (Ctrl+C), clipboard-ul nu mai este golit.
- **Un calculator compromis** (keylogger, malware) poate captura parola master la tastare.
- **Revenirea la o versiune mai veche a fișierului** (rollback): un backup vechi, valid, se deschide fără avertisment.

## Limitări cunoscute

- `Vault.create` suprascrie un seif existent; confirmarea există doar în meniu.
- Calea seifului e relativă la folderul curent.
- Numărul de iterații este fix: o eventuală creștere a lui ar face seifurile vechi imposibil de deschis
  până la adăugarea unei migrări (header-ul păstrează deja `kdf` și `iterations` pentru asta).
- Clipboard-ul folosește AWT, deci programul are nevoie de un mediu grafic (nu rulează prin SSH fără ecran).

## Îmbunătățiri posibile

- **Argon2id** în loc de PBKDF2 (memory-hard, mult mai rezistent la atacuri cu GPU), cu migrare după câmpul `kdf`
- teste pentru `Vault` (salvare → redeschidere, fișier modificat, folosire după `close()`)
- demonstrații de atac în teste: ECB (tipare vizibile), nonce refolosit în GCM, `java.util.Random` previzibil
- golirea clipboard-ului și la oprirea forțată (shutdown hook)

## Structura proiectului

```
src/main/java/ro/mateiavram/
├── crypto/   CryptoService, EncryptedData, PasswordGenerator
├── vault/    Vault, VaultFile, VaultHeader, Entry
└── pm/       Main (meniul), ClipboardHelper
src/test/java/ro/mateiavram/pm/
              CryptoServiceTest, OpenCloseTests, ChangePasswordTest
```
