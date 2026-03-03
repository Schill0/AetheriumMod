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

### 3. Smart Auto Bot (Integrazione Litematica & Pathfinding A*)
Questa è la parte automazione (`com.espmod.litematica.automation.*`), gestita dall'`AutomationEngine`. L'engine ticka ad ogni frame e devia l'esecuzione su due bot intelligenti, supportati da un nuovo motore di navigazione 3D locale.

#### 📦 Classi Principali del Sistema Bot:
- **`AutomationEngine.java`**: È il cuore pulsante dell'automazione. Si aggancia al `ClientTickEvents.END_CLIENT_TICK` e, a seconda del ruolo selezionato nella GUI (MINER, BUILDER, ecc.), reindirizza l'esecuzione alla rispettiva logica (es. `MinerLogic.tick()`). Gestisce anche i controlli di sicurezza (es. fermare il bot se si preme un tasto manualmente).
- **`MinerLogic.java`**: Macchina a stati (State Machine) per il ruolo del Miner. Gestisce la ricerca dei blocchi da rompere, la verifica dell'inventario per gli attrezzi e l'invio dei comandi di attacco (`attackKey`) per simulare il click sinistro del giocatore.
- **`BuilderLogic.java`**: Macchina a stati per il ruolo del Builder. Si occupa di confrontare i blocchi richiesti dalla schematica con l'inventario del player. Se mancano materiali, gestisce l'interazione con le casse (simulando l'apertura e lo Shift+Click sui GUI slot). Posiziona i blocchi girando la visuale e azionando il tasto destro (`useKey`).
- **`Pathfinder.java`**: Situato in `automation.pathfinding`, contiene l'algoritmo A* (A-Star). Riceve una start-pos e una target-pos, restituendo una `Queue<BlockPos>` (coda di blocchi) che rappresenta il percorso più sicuro, aggirando muri e calcolando salti/cadute. Contiene al suo interno la sottoclasse `PathNode` per il calcolo dei pesi geodetici (`gCost`, `hCost`).
- **`ChestAreaManager.java` & `MiningAreaManager.java`**: Gestori di persistenza che salvano/caricano le coordinate dei comandi `.zchest` e `.at` su file JSON locali (`config/espmod/`). Forniscono al Miner e al Builder i confini esatti su cui operare, slegandoli dalla sola lettura delle schematiche.
- **`LitematicaMainScreen.java`**: La GUI interattiva (accessibile dal menu dell'ESP) in cui l'utente avvia l'engine (`START/STOP`) e cambia il ruolo attivo del bot.

---


#### Il Motore di Movimento: Pathfinding A* (`Pathfinder.java`)
Il "cervello" di movimento della mod. A differenza di semplici bot che andavano "dritti verso il bersaglio", il nostro `Pathfinder`:
- Analizza l'ambiente calcolando in anticipo un percorso a blocchi (`Queue<BlockPos>`) usando l'algoritmo **A*** (A-Star).
- Gestisce salti (salite in verticale di max 1 blocco purché ci sia aria per la testa).
- Gestisce discese sicure (droppa dal bordo a un massimo di 3 blocchi per evitare danni fatali da caduta).
- Capisce gli ostacoli e genera percorsi aggirandoli in modo da evitare che il bot resti incastrato nei muri.

#### Il Miner (`MinerLogic`)
Scopo: Pulire l'area prima di costruire o minare a vuoto una zona custom.
- Trova discrepanze tra il mondo reale e la schematica Litematica (blocchi di troppo) e usa il `Pathfinder` per raggiungerli e spaccarli.
- **Feature Area Manuale (`.at set A` e `.at set B`)**: L'utente traccia un volume 3D (`MiningAreaManager`). Il Miner distruggerà tutto al suo interno. Qualora *non* ci sia una schematica Litematica caricata come guida, tratterà ogni singolo blocco nell'area come "blocco da spaccare".
- **Toolless Support**: Può minare persino a mani nude se non trova attrezzi nella ChestZone né nell'inventario, seppur con tempistiche più lente.

#### Il Builder (`BuilderLogic`)
Scopo: Trovare materiali e piazzare i blocchi fantasma delineati da Litematica.
- Sfrutta il `Pathfinder` per orientarsi sul terreno e percorrere impalcature complesse o salti verso il blocco da piazzare.
- **Area Casse (`.zchest A` e `.zchest B`)**: L'utente delimita una zona casse. Quando il Builder ha finito i blocchi o non ha quello richiesto dalla schematica, genera un percorso A* fino alla "cesta" più vicina, esegue routine di simulazione inventario (Shift+Click) per recuperare stack interi del blocco mancante, e torna al lavoro.

---

## ⏳ Cosa manca da fare (To-Do)

- [ ] **UI di Comando per Litematica Bot**: Attualmente `.at` e `.zchest` usano la Vanilla Chat per configurare i punti. Andrebbe integrato un menu grafico (dentro la gui di YACL) per gestire i vertici graficamente o con un marker visivo (particelle in game) sui punti A e B settati.
- [ ] **Scambio Attrezzi Miner intelligente**: Evitare che il Miner usi il piccone su blocchi di terra perdendo durabilità, ma forzarlo a scambiare slot per la pala al volo verificando il tag del blocco colpito.
- [ ] **Sincronizzazione Essentials Party**: Unire la lista blocchi e i Ghost Block rendering con i dati inviati dagli altri client in caso di coop massive con Essentials.

---

## 🛠️ Ulteriori Dettagli Tecnici

- Tutte le logiche (incluso il Pathfinder A*) girano lato **Client**, senza dipendenze sul server della partita.
- **`ClientTickEvents`**: Tutti i bot operano all'interno dell'`END_CLIENT_TICK`, comportandosi e inviando i pacchetti di update orientamento/pitch/yaw al server esattamente come farebbe un normalissimo giocatore umano.
- **Configurazioni persistenti (Gson)**: Tutte le aree e settaggi personalizzati (`.at` e `.zchest`) vengono salvate istantaneamente in json all'interno di `config/espmod/`, non vanno riconfigurate ad ogni restart.
