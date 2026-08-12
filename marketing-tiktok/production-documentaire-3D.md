# Production Documentaire 3D — Lissafi TikTok 🇳🇪

Production complète pour 21 vidéos TikTok en style documentaire 3D (type FERN).
Chaque vidéo inclut : script narrateur avec audio tags ElevenLabs, prompts images Leonardo.ai,
réglages voix ElevenLabs, instructions montage CapCut.

**Esthétique visuelle :** mannequins 3D blancs sans visage (sans yeux, nez, bouche, oreilles, cheveux),
corps anatomiquement réalistes, vêtements réalistes avec plis de tissu, fond épuré, lumière studio douce.
**Ton narratif :** documentaire cinématique intense, phrases courtes, présent, tension émotionnelle.

---

## ⚙️ CONFIGURATION GLOBALE — À LIRE AVANT DE COMMENCER

---

### 🎙️ ELEVENLABS — Guide Audio Tags & Réglages

#### Les Audio Tags (Eleven v3 uniquement)

Ces balises entre crochets donnent des **indications de jeu** au modèle. Elles ne sont **pas lues à voix haute**.
Place-les au début d'une phrase ou juste avant le mot concerné.

| Catégorie | Tags | Effet |
|---|---|---|
| **Émotions** | `[sad]`, `[angry]`, `[happily]`, `[worried]`, `[nervous]`, `[triumphant]`, `[melancholy]` | Charge émotionnelle de la phrase |
| **Respiration** | `[pause]`, `[long pause]`, `[sighs]`, `[clears throat]` | Silence et respiration naturelle |
| **Intensité** | `[whispers]`, `[shouts]`, `[softly]`, `[excited]` | Volume et énergie de la voix |
| **Ton** | `[dramatic tone]`, `[sarcastic]`, `[awe]`, `[somber]`, `[urgent]` | Couleur narrative de la phrase |

**Exemple :**
```
[sad] Chaque soir, il sort son cahier, il recompte. [long pause] Et souvent... ça tombe pas juste. [dramatic tone] Mais aujourd'hui, tout a changé.
```

#### Règles d'usage des tags

- Max 1-2 tags par phrase
- Place le tag **au début** de la phrase qu'il doit colorer
- `[pause]` = ~0.5s de silence, `[long pause]` = ~1.5s
- Ne mets JAMAIS de tag en plein milieu d'un mot
- Teste la génération 2-3 fois et garde la meilleure prise

#### Réglages recommandés pour le documentaire français

| Paramètre | Valeur | Pourquoi |
|---|---|---|
| **Modèle** | Eleven v3 (Turbo ou normal) | Le seul qui supporte les audio tags |
| **Voix** | `George` (tension/chaleur) ou `Daniel` (autorité/percutant) | Teste les deux sur 30 sec |
| **Stability** | 30-40% | Plus bas = plus expressif, plus haut = plus constant |
| **Style Exaggeration** | 75-90% | Amplifie les émotions des tags |
| **Speed** | 0.85-0.95 | Légèrement plus lent que le défaut, effet documentaire |
| **Language** | Français (verrouille-le dans les paramètres) | Garantit la phonétique française correcte |

---

### 🖼️ LEONARDO.AI — Configuration

1. Va sur https://leonardo.ai → compte gratuit (150 crédits/jour)
2. **Model :** Leonardo Creative ou PhotoReal
3. **Aspect Ratio :** 9:16 (Vertical — TikTok)
4. **Resolution :** 1024×1792
5. **Guidance :** 7-8
6. Colle le prompt tel quel → génère 2-3 versions → garde la meilleure

---

### ✂️ CAPCUT — Configuration montage

1. Importe toutes les images dans l'ordre
2. Importe le fichier audio ElevenLabs
3. Synchronise les changements d'image avec la voix (voir tableaux par vidéo)
4. Sous-titres auto : Texte → Sous-titres automatiques → Français
5. Style sous-titres : Police bold sans-serif, contour noir, 3-5 mots par ligne, centrés en bas
6. Musique de fond : Volume 20-30% pendant la voix, monte à 50% aux transitions
7. Export : 1080×1920 (9:16), 30 fps

---

## JOUR 1 — LUNDI (Lancement)

---

### VIDÉO 1.1 — « Combien on te doit ? »

---

#### 🎬 SCRIPT DOCUMENTAIRE — Avec Audio Tags ElevenLabs

Copie ce script TEL QUEL dans ElevenLabs (tags inclus). Modèle : Eleven v3.

```
[somber] Chaque matin, une boutique ouvre ses portes au marché de Niamey. [pause] Le commerçant sort son cahier. Il tourne les pages. Il cherche. [long pause] Quelque part dans ces pages, des dettes dorment. [sad] Des clients qui ont dit je paie demain... et qui ne sont jamais revenus. [worried] Il ne sait plus. Cinq mille ? Dix mille ? Vingt mille ? Les chiffres se mélangent dans les pages cornées. [melancholy] Ce flou... c'est de l'argent qui s'évapore. Chaque jour. Sans bruit. [long pause] [dramatic tone] Et puis un jour... il remplace le cahier par une application. [triumphant] Lissafi. Une caisse enregistreuse qui vit dans son téléphone. [softly] Là, chaque crédit a un nom. Un montant. Une date. Tout est là, en un clic. [pause] Il n'oublie plus. L'application n'oublie jamais. [pause] Écris-nous LISSAFI en message privé. C'est gratuit pour commencer.
```

**Durée estimée :** ~42-45 secondes

---

#### 🖼️ PROMPTS IMAGES LEONARDO.AI

Copie chaque prompt tel quel. Modèle : **Leonardo Creative** ou **PhotoReal**. Ratio : 9:16.

```
Ultra-realistic cinematic 3D rendered scene. A white featureless mannequin figure stands behind a wooden market counter in Niamey, early morning, warm golden light filtering through a corrugated metal roof. The mannequin wears a simple white t-shirt and dark shorts, no facial features, smooth featureless head. He holds an old worn notebook in his hands, flipping through pages. Dust particles floating in morning light. Dramatic natural lighting, shallow depth of field on the notebook. Clean composition, documentary film still, premium 3D quality, photorealistic fabric folds on the t-shirt.

Ultra-realistic cinematic 3D rendered scene. Close-up shot of weathered hands of a white mannequin figure holding a crumpled, dirty notebook with scribbled pages. Torn pages, crossed-out numbers visible. The notebook shows debt records — names and amounts written in messy handwriting, some crossed out, some faded. Soft studio lighting from the side creates long shadows on the pages. Mood: frustration, loss, things slipping away. Clean composition, shallow depth of field, documentary film aesthetic, premium 3D quality.

Ultra-realistic cinematic 3D rendered scene. White featureless mannequin figure sitting alone at a wooden table in a dark room, late evening. A single warm lamp illuminates the scene from above. The mannequin wears a white t-shirt, head smooth and blank. Stacks of crumpled CFA franc banknotes scattered on the table. Papers everywhere. His posture shows exhaustion — head tilted down toward the mess. Mood: fatigue, overwhelming, quiet desperation. Cinematic lighting, strong contrast between warm lamp and dark room. Premium 3D quality.

Ultra-realistic cinematic 3D rendered scene. Same white featureless mannequin figure now holding a smartphone in his hand, the phone screen glowing softly. The background is clean, minimal, a calm evening setting. The mannequin wears the same white t-shirt, smooth blank head tilted gently toward the phone. The phone screen displays a clean, organized interface — numbers, lists, everything in order. Mood: relief, clarity, peace after chaos. Soft diffused lighting, calm color palette transitioning from warm chaos to cool order. Premium 3D quality, photorealistic.

Ultra-realistic cinematic 3D rendered scene. Split composition. Left side: a white featureless mannequin in a white t-shirt looking down at a messy notebook, dark shadowy lighting, warm chaotic tones. Right side: the same mannequin holding a smartphone with a clean organized screen, calm cool lighting. The contrast between chaos and order. Both figures are identical, separated by a thin vertical light line. Studio lighting, dramatic contrast, premium 3D quality, documentary visual storytelling.
```

---

#### 🎙️ RÉGLAGES ELEVENLABS

| Paramètre | Valeur |
|---|---|
| **Modèle** | Eleven v3 (Turbo recommandé pour la vitesse) |
| **Voix** | `George` (chaleur + gravité) |
| **Stability** | 35% |
| **Style Exaggeration** | 85% |
| **Speed** | 0.90 |
| **Language** | Français (verrouillé) |

---

#### ✂️ INSTRUCTIONS MONTAGE — CAPCUT

| Timing approx. | Visuel | Audio |
|---|---|---|
| 0:00-0:09 | Image 1 (comptoir, cahier) — zoom in très lent | Voix off seule, ambiance marché très basse |
| 0:09-0:16 | Image 2 (gros plan cahier, pages cornées) — coupe franche | Voix off, ambiance coupe net |
| 0:16-0:22 | Image 3 (mannequin fatigué, table, billets) — fondu enchaîné | Voix off + musique douce qui monte (piano mineur) |
| 0:22-0:27 | Image 4 (mannequin avec téléphone, calme) — coupe franche, flash blanc subtil | Voix off + musique s'ouvre, plus lumineuse |
| 0:27-0:42 | Image 5 (split chaos/ordre) — plan fixe, zoom out très lent | Voix off + musique pleine |
| 0:42-0:45 | Fond noir + texte « Écris-nous LISSAFI en privé » | Dernière phrase, musique coupe net |

**Musique :** Cherche « tension documentary piano » ou « cinematic ambient suspense » dans CapCut.

---

### VIDÉO 1.2 — « Mode avion »

---

#### 🎬 SCRIPT DOCUMENTAIRE — Avec Audio Tags ElevenLabs

```
[urgent] Le marché de Niamey. La foule. Les toits en tôle. Les murs épais. [pause] Et soudain... plus rien. [dramatic tone] Zéro barre. Le réseau est mort. [sad] Dans une boutique au fond du marché, un commerçant regarde son téléphone. Pas de connexion. Pas de données. Rien ne passe. [long pause] [somber] Normalement... c'est là que tout s'arrête. [worried] Les applications modernes ont besoin du cloud. Pas de réseau, pas de vente. [dramatic tone] Mais lui... [pause] il active autre chose. [triumphant] Il passe en mode avion. Et il continue à travailler. [excited] Il scanne un article. Le total s'affiche. Il encaisse. Ticket. [softly] Parce que sa caisse ne vit pas dans les nuages. Elle vit ici. Dans son téléphone. [pause] Lissafi. Une caisse qui marche là où le réseau ne va pas. [pause] Quand le réseau revient, tout se synchronise. Automatiquement. Sans lui. [pause] Écris-nous LISSAFI en message privé. C'est gratuit pour tester.
```

**Durée estimée :** ~50-52 secondes

---

#### 🖼️ PROMPTS IMAGES LEONARDO.AI

```
Ultra-realistic cinematic 3D rendered scene. Wide establishing shot of a busy African market in Niamey, Niger. White featureless mannequin figures scattered throughout — some walking, some standing at stalls. All wearing simple white t-shirts and dark shorts, smooth featureless heads. Corrugated metal roofs overhead, narrow alleys, vibrant fabric stalls, dust in the air. Harsh midday sunlight creating strong shadows. Mood: energy, chaos, everyday life. Cinematic composition, documentary film feel, premium 3D quality, photorealistic fabrics and textures.

Ultra-realistic cinematic 3D rendered scene. Over-the-shoulder shot of a white featureless mannequin figure in a white t-shirt standing at a wooden market stall. He holds a smartphone in his hands, staring at the screen. The phone screen shows NO SIGNAL — empty signal bars, a red warning indicator. The mannequin's posture shows frustration — shoulders tense. Background is dark, cramped, corrugated metal walls. Harsh top lighting, dramatic shadows. Mood: frustration, helplessness, technology failing. Premium 3D quality.

Ultra-realistic cinematic 3D rendered scene. Close-up of a hand of a white featureless mannequin figure reaching toward a smartphone screen, finger about to tap the AIRPLANE MODE icon. The icon glows orange/amber. Dark background, the phone screen illuminates the scene. Dramatic spotlight on the phone. Mood: decision, turning point, deliberate action. Shallow depth of field, cinematic lighting, premium 3D quality.

Ultra-realistic cinematic 3D rendered scene. White featureless mannequin figure in a white t-shirt at a market stall, holding a smartphone with a barcode scanning interface visible on screen. The scan line is sweeping across. A small red laser dot from the phone camera hits a product on the counter. The phone screen shows items being added to a list, prices appearing. Cool blue phone light contrasts with warm market ambient. Mood: focus, technology working against the odds. Premium 3D quality, cinematic framing.

Ultra-realistic cinematic 3D rendered scene. Same white featureless mannequin figure now looking at the phone screen showing a completed receipt/ticket. Digital ticket with items, prices, total, date — clean and organized. The mannequin's posture is relaxed now, confident. In the background, the signal bars slowly reappear on the phone status bar with a small green checkmark. Mood: relief, victory, quiet confidence. Soft lighting, calm atmosphere. Premium 3D quality.

Ultra-realistic cinematic 3D rendered scene. Symbolic wide shot. A white featureless mannequin figure stands in the middle of a busy market at sunset, holding a glowing smartphone upward like a lantern. Around him, the market continues — blurred motion of other mannequins. The phone glows warm golden light. Signal waves radiate from the phone as subtle light rings. Mood: empowerment, hope, the future arriving. Cinematic golden hour lighting, epic composition, premium 3D quality.
```

---

#### 🎙️ RÉGLAGES ELEVENLABS

| Paramètre | Valeur |
|---|---|
| **Modèle** | Eleven v3 Turbo |
| **Voix** | `Daniel` (autorité, intensité) |
| **Stability** | 30% |
| **Style Exaggeration** | 88% |
| **Speed** | 0.92 |
| **Language** | Français (verrouillé) |

---

#### ✂️ INSTRUCTIONS MONTAGE — CAPCUT

| Timing approx. | Visuel | Audio |
|---|---|---|
| 0:00-0:10 | Image 1 (marché animé) — pan lent droite → gauche | Voix off + bruit fond marché (20%) |
| 0:10-0:18 | Image 2 (NO SIGNAL, frustration) — coupe | Voix off, bruit fond coupe → silence |
| 0:18-0:24 | Image 3 (doigt sur mode avion) — zoom in rapide | Voix off + son « bip » mode avion |
| 0:24-0:28 | Flash blanc de transition | Silence 0.5s → musique démarre |
| 0:28-0:37 | Image 4 (scan, caissier) — 3 cuts rythmés | Voix off + bips scan + musique |
| 0:37-0:47 | Image 5 (ticket, barres réseau vertes) — fondu | Voix off + musique pleine |
| 0:47-0:52 | Image 6 (téléphone lanterne, sunset) — zoom out | Voix off + musique baisse |
| 0:52-0:55 | Fond noir + texte CTA | Dernière phrase, musique coupe net |

**Musique :** Cherche « tension buildup cinematic » dans CapCut. Volume 25% fond, 60% climax.

---

### VIDÉO 1.3 — « C'est quoi Lissafi ? »

---

#### 🎬 SCRIPT DOCUMENTAIRE — Avec Audio Tags ElevenLabs

```
[softly] Tu tiens une boutique. Tu vends du riz, des épices, du savon. [pause] Et tous les soirs... [sad] tu fais la même chose. Tu sors ton cahier. Tu comptes les billets. Tu additionnes. [melancholy] Ce rituel dure des heures. Et souvent... ça ne tombe pas juste. [long pause] [dramatic tone] Mais il existe maintenant quelque chose de différent. [triumphant] Une caisse enregistreuse. Dans ton téléphone. [excited] Tu scannes un article. Le total se calcule. La monnaie aussi. Ticket. [pause] [softly] Et ce n'est que le début. Tes crédits clients. Ton stock. Tes rapports du soir. Tout au même endroit. Automatique. [pause] [awe] Cette application s'appelle Lissafi. [pause] Elle est gratuite pour commencer. Aucun engagement. Écris-nous LISSAFI en message privé. On commence quand tu veux.
```

**Durée estimée :** ~43-46 secondes

---

#### 🖼️ PROMPTS IMAGES LEONARDO.AI

```
Ultra-realistic cinematic 3D rendered scene. Interior of a small neighborhood shop in Niamey. A white featureless mannequin figure in a white t-shirt and dark shorts stands behind a wooden counter, arranging products — bags of rice, small spice packets, bars of soap on shelves. Warm interior lighting, afternoon sun through a doorway. The mannequin looks toward the entrance. Realistic product textures, dust motes in the light. Mood: everyday dignity, honest work. Premium 3D quality, documentary film still.

Ultra-realistic cinematic 3D rendered scene. Night time. Same white featureless mannequin figure in a white t-shirt sits at a small wooden table in a dimly lit back room. A single bare bulb hangs overhead. He hunches over an open notebook, counting crumpled CFA franc banknotes spread across the table. A small calculator nearby. His posture is tired, shoulders slumped. Empty tea glass on the side. Mood: exhaustion, late-night grind, quiet struggle. Dramatic single-source lighting, strong shadows. Premium 3D quality.

Ultra-realistic cinematic 3D rendered scene. White featureless mannequin figure in a white t-shirt at the same market counter, but now holding a smartphone. The phone screen glows with a clean, modern interface — a digital cash register layout with product scanner, running total, and a large button. The mannequin points the phone camera at a product on the counter. Mood: transition, discovery, the simplicity of the solution. Soft clean lighting, calm atmosphere. Premium 3D quality.

Ultra-realistic cinematic 3D rendered scene. Close-up of a smartphone held in the hands of a white featureless mannequin figure. The phone screen displays a completed digital receipt — items listed, prices, total (12,200 FCFA), amount given (15,000 FCFA), change (2,800 FCFA), date and time. Clean Sans-serif typography on the screen, organized layout. The receipt looks professional. Shallow depth of field with the phone screen in perfect focus. Premium 3D quality.

Ultra-realistic cinematic 3D rendered scene. Montage composition showing the smartphone screen transforming into four floating panels around a white featureless mannequin figure's hand. Each panel shows a different app screen: sales register, client debts list, stock inventory with alerts, daily revenue report. The panels glow softly, clean organized data. The mannequin stands confidently in the center. Dark studio background. Mood: empowerment, control, everything in one place. Cinematic studio lighting, premium 3D quality.
```

---

#### 🎙️ RÉGLAGES ELEVENLABS

| Paramètre | Valeur |
|---|---|
| **Modèle** | Eleven v3 Turbo |
| **Voix** | `George` (chaleureux, proche) |
| **Stability** | 40% |
| **Style Exaggeration** | 78% |
| **Speed** | 0.88 |
| **Language** | Français (verrouillé) |

---

#### ✂️ INSTRUCTIONS MONTAGE — CAPCUT

| Timing approx. | Visuel | Audio |
|---|---|---|
| 0:00-0:05 | Image 1 (boutique, jour) — plan fixe | Voix off + ambiance douce |
| 0:05-0:17 | Image 2 (nuit, cahier, fatigue) — zoom in très lent | Voix off, ambiance baisse |
| 0:17-0:24 | Image 3 (même comptoir, téléphone) — coupe + flash transition | Voix off + léger whoosh |
| 0:24-0:29 | Image 4 (gros plan ticket) — plan fixe | Voix off + bip scan discret |
| 0:29-0:46 | Image 5 (4 panneaux flottants) — zoom out lent, rotation subtile | Voix off + musique inspirante |
| 0:46-0:48 | Fond noir + texte CTA | Dernière phrase, musique fade out |

**Musique :** Cherche « hopeful piano documentary » ou « cinematic inspiration soft » dans CapCut.

---

---

## 📋 Planning de production — Semaine 1

| Jour | Vidéos | Statut |
|---|---|---|
| **Lundi** | 1.1 Combien on te doit / 1.2 Mode avion / 1.3 C'est quoi | ✅ Prêt |
| Mardi | 2.1 Je paie demain / 2.2 Stock / 2.3 Ce que le cahier coûte | ⏳ |
| Mercredi | 3.1 Ticket WhatsApp / 3.2 Juste un téléphone / 3.3 Tague | ⏳ |
| Jeudi | 4.1 Le soir / 4.2 Vente en direct / 4.3 Pourquoi Lissafi | ⏳ |
| Vendredi | 5.1 Calcul / 5.2 Marché vs bureau / 5.3 Installation | ⏳ |
| Samedi | 6.1 Question / 6.2 Commerçant pas comptable / 6.3 Reprise | ⏳ |
| Dimanche | 7.1 Deux types / 7.2 Résumé / 7.3 Demain | ⏳ |
