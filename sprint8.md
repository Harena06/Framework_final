# resaka upload 

Rehefa tonga any amin'ny serveur de afaka jerena hoe misy amin'le zavatra tonga avy any amin'ny client ve type==file

Rehefa tonga de misafidy le dev hoe stockena anaty serveur ve sa stockena anaty base

Mi creer classe vaovao
MapFile

Anarany, Extension, Tableau ana Byte[]
De io no instanciena aty amin'ny cote framework
Rehefa maita match (Binding) anaty Formulaire de mijery ny attributs an'le methode (Ohatra hoe mila mapFile) de mi creer instance ana MapFile,
Dec fenoina daoly le parametre an'le methode exactement  comme tel fa mapFile no fenoina


# Sprint 9
Session

Simple : il faut que la session de l'utilisateur soit disponible dans le controller de developpeur comme ca il peut faire ce qu'il veut avec

On ne touche pas au authorisations pour le moment

Pour le moment :

Il faut creer un nouveau Classe (dto.Session)
Il y a un map dans cette classe et des methodes utils (get() et set())
La classe n'est pas encore relier a HttpSession mais juste une classe ordinaire

FrontControllerServlet : si il voit qu'il y a un parametre Session avant de passer la session

On boucle l'HttpSession et on remplace toutes les valeurs de cette session dans le Map, si il en existe

Rehefa tonga ao any amin'ny dev le Session de averina tsindrina le Map avy eo

Amin'izay le Map an'le classetsika no lalaoviny fa tsy milalao HttpSession le dev