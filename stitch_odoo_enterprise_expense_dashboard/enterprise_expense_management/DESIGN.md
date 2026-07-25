---
name: Enterprise Expense Management
colors:
  surface: '#f8f9fa'
  surface-dim: '#d9dadb'
  surface-bright: '#f8f9fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4f5'
  surface-container: '#edeeef'
  surface-container-high: '#e7e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#43474e'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#f0f1f2'
  outline: '#73777f'
  outline-variant: '#c3c6cf'
  surface-tint: '#416084'
  primary: '#002645'
  on-primary: '#ffffff'
  primary-container: '#1a3c5e'
  on-primary-container: '#87a7ce'
  inverse-primary: '#a9c9f2'
  secondary: '#006687'
  on-secondary: '#ffffff'
  secondary-container: '#87d6fe'
  on-secondary-container: '#005d7c'
  tertiary: '#352100'
  on-tertiary: '#ffffff'
  tertiary-container: '#513500'
  on-tertiary-container: '#c79e60'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d1e4ff'
  primary-fixed-dim: '#a9c9f2'
  on-primary-fixed: '#001d36'
  on-primary-fixed-variant: '#28496b'
  secondary-fixed: '#c1e8ff'
  secondary-fixed-dim: '#81d0f8'
  on-secondary-fixed: '#001e2b'
  on-secondary-fixed-variant: '#004d67'
  tertiary-fixed: '#ffddb0'
  tertiary-fixed-dim: '#ecbf7e'
  on-tertiary-fixed: '#281800'
  on-tertiary-fixed-variant: '#5f410b'
  background: '#f8f9fa'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.3'
  headline-sm:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: '1.4'
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: '1.5'
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: '1'
    letterSpacing: 0.05em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: '1.2'
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  container-padding: 24px
  gutter: 16px
  sidebar-width: 280px
  card-gap: 20px
---

## Brand & Style

Ce système de design est conçu pour une gestion rigoureuse et professionnelle des notes de frais au sein de l'écosystème Odoo. L'objectif est d'instaurer une confiance immédiate tout en optimisant l'efficacité opérationnelle des comptables et des employés.

L'esthétique adoptée est **Corporate / Moderne**, mettant l'accent sur la clarté de l'information et la structure hiérarchique. Le style privilégie une interface aérée (whitespace) pour réduire la charge cognitive lors de la manipulation de données financières complexes. L'approche est utilitaire mais raffinée, garantissant une lisibilité maximale pour les transactions en FCFA.

## Colors

La palette de couleurs est ancrée dans le sérieux institutionnel avec un bleu profond (`#1A3C5E`) comme couleur primaire, symbolisant la stabilité. La couleur d'accentuation bleue claire est réservée aux actions secondaires et aux éléments interactifs.

Les indicateurs de statut utilisent des teintes sémantiques strictes : le vert pour les validations de dépenses, et l'orange pour les alertes de politique de frais ou les attentes de justificatifs. Les fonds de page utilisent des gris extrêmement clairs pour séparer visuellement les cartes de données de l'arrière-plan global.

## Typography

Le système utilise exclusivement **Inter** pour sa lisibilité exceptionnelle sur écran et son aspect systématique. 

- Les titres utilisent un graisse `600` ou `700` avec un espacement de caractères légèrement réduit pour une apparence dense et professionnelle.
- Le corps de texte standard est fixé à `14px` pour permettre l'affichage d'un grand volume de données sans encombrement.
- Les étiquettes (labels) de colonnes et de statuts utilisent une version condensée en majuscules pour les différencier des données saisies.

## Layout & Spacing

Le layout repose sur une structure de **grille fluide à 12 colonnes** avec des marges de sécurité fixes. 

1.  **Sidebar :** Une barre latérale fixe de `280px` à gauche contient la navigation principale.
2.  **Dashboard Content :** Une zone principale avec un padding interne de `24px`.
3.  **Grille de Cartes :** Les indicateurs clés (KPIs) sont disposés en une ligne de 4 colonnes sur desktop, passant à 2 colonnes sur tablette.
4.  **Data Tables :** Les tableaux de dépenses occupent 100% de la largeur disponible pour maximiser la visibilité des montants en FCFA et des descriptions.

L'unité de base de `8px` régit tous les espacements internes (paddings et margins) pour maintenir une cohérence visuelle rigoureuse.

## Elevation & Depth

La hiérarchie visuelle est établie par des **couches tonales** et des ombres portées subtiles. 

- **Niveau 0 (Fond) :** Gris neutre très clair (`#F8F9FA`).
- **Niveau 1 (Cartes et Tableaux) :** Blanc pur avec une bordure fine de `1px` (`#DEE2E6`) et une ombre portée "Soft" (0px 2px 4px rgba(0,0,0,0.05)).
- **Niveau 2 (Survol / Modales) :** Ombre plus diffuse pour simuler la proximité de l'utilisateur.

L'utilisation de flous de profondeur est évitée pour conserver la netteté nécessaire à une application de productivité financière.

## Shapes

Conformément aux standards modernes d'Odoo, tous les éléments interactifs et les conteneurs utilisent des coins arrondis de **8px** (`roundedness: 2`).

- **Boutons & Inputs :** Radius de 8px pour une sensation de modernité accessible.
- **Pills de statut :** Rayon maximal (Pill-shaped) pour contraster avec la structure orthogonale des tableaux de données.
- **Cartes KPI :** Radius de 8px pour encadrer proprement les graphiques et les chiffres clés.

## Components

### Sidebar de Navigation
Utilise le fond `Primary Color`. Les liens actifs possèdent une bordure gauche de 4px de couleur `Accent` et un changement d'opacité sur le texte.

### Cartes KPI
Chaque carte affiche un titre de catégorie, le montant total en **FCFA** (en gras), et un indicateur de tendance (flèche verte ou rouge) avec le pourcentage de variation par rapport au mois précédent.

### Tableaux de Données (OWL Style)
- **Header :** Fond gris clair, texte en `label-md`.
- **Lignes :** Alternance de couleurs (zebra striping) très légère.
- **Pills de Statut :** Texte coloré sur fond de couleur à 15% d'opacité (ex: texte vert sur fond vert clair pour "Approuvé").

### Champs de Saisie (Inputs)
Bordure de 1px grise, devenant `Accent Color` lors du focus. Les libellés sont toujours placés au-dessus du champ pour une lecture verticale rapide.

### Graphiques
Utilisation de graphiques linéaires ou à barres simplifiés pour les dépenses mensuelles, utilisant la palette de couleurs primaire et secondaire pour assurer une cohérence visuelle.