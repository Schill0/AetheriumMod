# Documentazione EspMod & Smart Auto Bot

EspMod è una mod per Minecraft Fabric (1.21.1) sviluppata con funzionalità avanzate che spaziano da utility grafiche (ESP, Freecam, Freelook) a sistemi di automazione completi per Litematica e analisi AI dei chunk.

---

## 🏗️ Architettura Generale (Cosa c'è)

La mod è divisa in tre grandi macro-strutture:

### 1. Moduli Utility Visive (ESP, Render, Camera)
- **Freecam & Freelook**: Permettono di sganciare la telecamera dal corpo del giocatore per esplorare o guardarsi attorno liberamente senza ruotare la hitbox del player.
- **ESP (Extra Sensory Perception)**: Usa il modulo `TargetManager` per evidenziare entità (Mob, Player, Animali) o blocchi (Spawners, Chest) attraverso i muri con linee tracer e box colorati.
- **YACL Config GUI (`EspModConfigYacl`)**: Un'interfaccia basata su *Yet Another Config Lib* (YACL) accessibile premendo l'apposito tasto, che permette di configurare in tempo reale i ritardi, i colori, i target dell'ESP e i toggle delle varie funzionalità.

### 2. Analisi Avanzata Chunk (ChunkScanner & AI)
I moduli `ChunkAnalysisManager` e `ChunkAnalysisAiManager` permettono di estrapolare la geometria dei chunk (i blocchi) e convertirli in array da dare in pasto ad un modello di Intelligenza Artificiale basato su **ONNX Runtime**.
- **Scopo**: Classificare la probabilità che determinati minerali (es. Diamanti) o strutture si trovino in quel chunk senza dover scavare.
- **Come funziona**: Si passa una matrice float di blocchi al modello AI locale (`.onnx`), il quale restituisce una probability map colorando visivamente il chunk.

### 3. Smart Auto Bot (Rimosso Temporaneamente)
Questa parte è stata recentemente sradicata per problematiche di sincronizzazione multiplayer. Prevedeva bot automatici (Miner e Builder) guidati da un sistema di Pathfinding A*.
Verrà riprogettata in futuro.

---

## ⏳ Cosa manca da fare (To-Do)

- [ ] **Nuovo Sistema Automazione**: Ripensare un engine di movimento affidabile in multiplayer.
- [ ] **Sincronizzazione Essentials Party**: Condividere le coordinate ESP e target tra i membri del party usando l'API di Essentials.

---

## 🛠️ Ulteriori Dettagli Tecnici

- Tutte le logiche (incluso il Pathfinder A*) girano lato **Client**, senza dipendenze sul server della partita.
- **`ClientTickEvents`**: Tutti i bot operano all'interno dell'`END_CLIENT_TICK`, comportandosi e inviando i pacchetti di update orientamento/pitch/yaw al server esattamente come farebbe un normalissimo giocatore umano.
- **Configurazioni persistenti (Gson)**: Tutte le aree e settaggi personalizzati (`.at` e `.zchest`) vengono salvate istantaneamente in json all'interno di `config/espmod/`, non vanno riconfigurate ad ogni restart.
