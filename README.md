# OnionRouting2


# Introduksjon
I dette prosjektet ble vi bedt om å implementere en Onion router. Poenget med onion router er at meldingen blir sendt mellom flere forksjellige noder før den når fram til målet sitt. Meldingen blir kryptert en gang for hver node. Når noden dekrypterer meldingen den får, vil den få instrukser over hvor neste node den skal sendes til. Resten av meldingen vil være kryptert, og vil bli dekryptert av neste node. På denne måten vil ikke nodene vite start og slutt node som man kan med http og https. Istedenfor vil nodene bare vite forrige og neste node. Ettersom jeg jobbet alene og ikke hadde kjempelang tid, er produktet mer av et proof of concept enn et polert produkt. Likevel har produktet flere deler av den basice funksjonaliten som man kan ønske av en onion router.

# Funksjonalitet og oppsett
Javafilene i programmet kan deles inn i fire hoveddeler:
1. Nodeklassene:
Det er tre klasser som sender meldinger til hverandre: OnionServer, OnionClient, og OnionNode. OnionClient er klient programmet og er programmet som gjør forespørselene til brukeren. OnionServer er programmet som simulerer en server som brukeren kan koble til. Det er i tillegg serveren som sender forespørsler ut på internettet. OnionNode er nodene som meldingene mellom OnionServer og OnionClient. Det er mange OnionNodes som kjører samtidig, slik at det er mange forskjellige ruter meldingene kan ta.
2. Foreldreklassene:
Ettersom de tre nodeklassene har mye til felles, ble prosjektet både enklere og penere av å implemetere klasser med metoder som flere klasser bruker som foreldreklasser. Den første foreldreklassen er OnionParent. Den inneholder metoder for sending og mottaking av data, pluss diverse metoder for håndtering av byte-arrays. Alle tre nodeklassene arver fra OnionParent (Selv om noen gjør det indirekte.) Den andre foreldreklassen er OnionEndPoint. Denne inneholder instrukser for hvordan å å velge ut sett moder noder og så kryptere meldingen for alle nodene. Den inneholder i tillegg metoder for å få tak i nøkler fra alle nodene. Denne klassen arver fra OnionParent og er arvet av OnionClient og OnionServer. På den måten arver alle tre nodeklassene fra OnionParent mens bare OnionServer og OnionClient arver fra OnionEndPoint.
3. MainKlasse:
Mainklassen OnionMain setter opp nodene, og setter så opp klienten og serveren, ellers har den ikke noen annen funksjon.
4. Enumeratorklasse:
Alle meldingene som blir sendt i produktet starter med en byte som signifiserer hvilke type melding det er. F.eks. nøkkelbytte, serverforespørsel, webforespørsel eller oppdelt svar. MessageNode er en enumerator som gir navn til de forskjellige byte-verdiene. Produktet hadde funket uten denne klassen, men å ha den med gjør at programmene blir lettere å skrive og lese da man ikke trenger å huske hvilken byte-verdi som var server forespørsel og hvilken som var web-forespørsel.

# Avhengigheter

# Instruksjoner

# Fremtidig arbeid

# Lenke til Repo
Lenke til Github repo: https://github.com/biasedLiar/OnionRouting2
I repoen kan man ikke se de tidligste committene. 
Det er fordi jeg lagde originalt prosjekt inni en annen repo. Problemet var at jeg med et uhell valgte feil mappe some root, så resultatet ble at det var to forskjellige prosjekter i samme repo. Når jeg oppdaget problemet flyttet jeg filene til en ny mappe og lagde en ny repo. Hvis man er interressert i å se på selve utviklingen av prosjektet, kan man finne det i repoen: https://github.com/biasedLiar/OnionRouting
Merk at root katalogen til prosjektet er under mappen Onionproject, og at de andre filene og mappene ikke tilhører dette prosjektet.
