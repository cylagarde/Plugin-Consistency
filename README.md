# Plugin consistency
Plugin consistency v2.x.x

## Installation
```
https://raw.githubusercontent.com/cylagarde/Plugin-Consistency/master/cl.plugin.consistency.update_site
```

## Introduction
Le plugin propose de v�rifier si un plugin a le droit d'avoir un autre plugin comme d�pendance.<br>
Par exemple, si vous avez 1 plugin qui d�finit des interfaces d'API et un autre plugin qui d�finit les impl�mentations des API, alors vous pouvez ne pas autoriser le plugin qui d�finit l'API � d�pendre du plugin qui d�finit les impl�mentations.<br>

## Pr�sentation
Le plugin affiche un button dans la barre d'outil principale avec 3 actions:<br>
<img src="./document/toolbar.png"/><br>
* <b>Launch consistency check</b> : Lancement de la v�rification des r�gles sur la consistence des plugins de votre workspace. La d�tection des incoh�rences sera affich�e sous forme de markers dans la vue "Problems' d'Eclipse.
* <b>Remove all consistencies check</b> : Effacement des markers trouv�s dans l'action pr�d�dente.
* <b>Open preference</b> : Ouverture de la pr�f�rence pour d�finir les r�gles de d�tection des incoh�rences. La pr�f�rence se trouve sous Window/Preferences/Plug-in Development/Plugin Consistency.

## Types Preference
Cette vue permet de d�finir un type de plugin.<br>
<img src="./document/types preference.png"/><br>
Ici, 4 types sont d�finis:
* API: le type pour les plugins qui d�finit les interfaces d'API.
* IMPLEMENTATION: le type pour les plugins qui d�finit les impl�mentations.
* PROJECT 1: le type pour les plugins concernant le project 1.
* PROJECT 2: le type pour les plugins concernant le project 2.<br>

L'�dition permet juste de donner un nom signification pour le type ainsi qu'une description d�taill�e.<br>
<img src="./document/edit type.png"/><br>

## Patterns Preference
Cette vue permet de d�finir un pattern de plugin.<br>
<img src="./document/patterns preference.png"/><br>
Ici, 4 patterns sont d�finis:
* la 1�re ligne indique que tous les plugins respectant le pattern "*.api" auront le type API et qu'ils ne pourront pas utiliser les plugins de type IMPLEMENTATION dans leurs d�pendances.
* la 2�me ligne indique que tous les plugins respectant le pattern "*.impl" auront le type IMPLEMENTATION.
* la 3�me ligne indique que tous les plugins respectant le pattern "project1.*" auront le type PROJECT1 et qu'ils ne pourront pas utiliser les plugins de type PROJECT2 dans leurs d�pendances.
* la 4�me ligne indique que tous les plugins respectant le pattern "project2.*" auront le type PROJECT1 et qu'ils ne pourront pas utiliser les plugins de type PROJECT1 dans leurs d�pendances.

L'�dition d'un pattern permet d'indiquer un ensemble de plugins avec une expression r�guli�re pour lui associer un type d�finit dans la vue pr�c�dente.<br>
<img src="./document/edit pattern.png"/><br>

## Plugins Preference
Cette vue est la plus importante, c'est elle qui d�finit les r�gles pour d�tecter les incoh�rences.<br>
<img src="./document/plugins preference.png"/><br>


## Workspace
Ici mon workspace constitu� de 2 projets distinct, chaque projet d�finit un plugin de d�finition des interfaces d'API et un plugin d'impl�mentation des interfaces.<br>
<img src="./document/workspace.png"/><br>

Dans le manifest du projet2 api, j'ai rajout� par erreur une d�pendance sur l'impl�mentation du projet 1.<br> 
<img src="./document/manifest project2 api.png"/><br>

Dans le manifest du projet2 impl, j'ai rajout� par erreur une d�pendance sur la d�finition d'interface du projet 1.<br> 
<img src="./document/manifest project2 impl.png"/><br>

Un clic sur le bouton permet de lancer une v�rification des incoh�rences.<br>
<img src="./document/dialog error.png"/><br>
<img src="./document/workspace avec erreur.png"/><br>

## Problems view
La vue "Problems' d'Eclipse permet d'afficher les incoh�rences d�tect�es dans les plugins du workspace.<br>
<img src="./document/problem view.png"/><br>
<br>
Un double clic sur une ligne ouvre le fichier MANIFEST.MF pour voir l'erreur.<br>
<img src="./document/manifest.png"/><br>
