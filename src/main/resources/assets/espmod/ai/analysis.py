import json
import torch
import torch.nn as nn
import numpy as np

# ==========================================
# 1. DEFINIZIONE E CARICAMENTO DEL MODELLO
# ==========================================

# ⚠️ SOSTITUISCI QUESTA CLASSE CON LA TUA VERA RETE NEURALE
class ReteNeuraleFinta(nn.Module):
    def forward(self, x):
        # Finge di fare un calcolo e sputa fuori un numero a caso (es. 0.85)
        return torch.tensor([[0.85]]) 

# Configura il dispositivo
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# Crea l'istanza del modello e caricalo in memoria
model = ReteNeuraleFinta().to(device)

# Se hai i pesi salvati, dovresti de-commentare questa riga:
# model.load_state_dict(torch.load("modello_addestrato.pth"))

model.eval() # Imposta il modello in modalità valutazione (fondamentale!)


# ==========================================
# 2. FUNZIONE DI PREDIZIONE
# ==========================================

def predict_chunk(json_string):
    try:
        # Pulizia base e caricamento
        json_string = json_string.strip()
        data = json.loads(json_string)
        
        if 'blocks' not in data:
            print("❌ Errore: 'blocks' non trovato.")
            return
            
        grid = np.array(data['blocks'])
        d1, d2, d3 = grid.shape
        target_dim = 51
        
        # Adattamento dimensioni
        if d2 > target_dim:
            grid = grid[:, :target_dim, :]
        elif d2 < target_dim:
            grid = np.pad(grid, ((0, 0), (0, target_dim - d2), (0, 0)), mode='constant')
            
        # Preparazione Tensori
        grid_tensor = torch.tensor(grid, dtype=torch.long)
        one_hot = torch.nn.functional.one_hot(grid_tensor, num_classes=4)
        one_hot = one_hot.permute(3, 0, 1, 2).float().unsqueeze(0) 
        
        # Predizione vera e propria (ora 'model' esiste!)
        with torch.no_grad():
            output = model(one_hot.to(device))
            probabilita = torch.sigmoid(output).item() * 100 if output.dim() <= 1 else output.item() * 100
            
        # Stampa risultato pulito
        print(f"\n--- 📡 RISULTATO ANALISI RADAR ---")
        print(f"Probabilità Base: {probabilita:.2f}%")
        
        if probabilita > 80:
            print("🚨 ALLERTA: Struttura artificiale rilevata con alta confidenza!")
        elif probabilita > 40:
            print("⚠️ SOSPETTO: Possibili tracce di attività umana o anomalie.")
        else:
            print("🌳 NATURALE: Il chunk sembra pulito.")

            return probabilita
            
    except Exception as e:
        # Qui stampiamo un errore MOLTO BREVE, così non intasa il terminale
        print(f"\n❌ Errore durante l'elaborazione (Dettaglio: {type(e).__name__})")

# ==========================================
# 3. AVVIO
# ==========================================
if __name__ == "__main__":
    print("Incolla la riga JSON (Premi Invio per analizzare):")
    # Disabilitiamo l'eco lungo nascondendo un po' di output visivo se crasha
    test_string = input()
    if test_string.strip():
        predict_chunk(test_string)