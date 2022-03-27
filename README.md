# OnionRouting2


## Introduksjon
I dette prosjektet ble vi bedt om å implementere en Onion router. Poenget med onion router er at meldingen blir sendt mellom flere forksjellige noder før den når fram til målet sitt. Meldingen blir kryptert en gang for hver node. Når noden dekrypterer meldingen den får, vil den få instrukser over hvor neste node den skal sendes til. Resten av meldingen vil være kryptert, og vil bli dekryptert av neste node. På denne måten vil ikke nodene vite start og slutt node som man kan med http og https. Istedenfor vil nodene bare vite forrige og neste node. Ettersom jeg jobbet alene og ikke hadde kjempelang tid, er produktet mer av et proof of concept enn et polert produkt. Likevel har produktet flere deler av den basice funksjonaliten som man kan ønske av en onion router.

## Funksjonalitet og oppsett
### Javafilene i programmet kan deles inn i fire hoveddeler:
1. Nodeklassene:  
Det er tre klasser som sender meldinger til hverandre: OnionServer, OnionClient, og OnionNode. OnionClient er klient programmet og er programmet som gjør forespørselene til brukeren. OnionServer er programmet som simulerer en server som brukeren kan koble til. Det er i tillegg serveren som sender forespørsler ut på internettet. OnionNode er nodene som meldingene mellom OnionServer og OnionClient. Det er mange OnionNodes som kjører samtidig, slik at det er mange forskjellige ruter meldingene kan ta.
2. Foreldreklassene:  
Ettersom de tre nodeklassene har mye til felles, ble prosjektet både enklere og penere av å implemetere klasser med metoder som flere klasser bruker som foreldreklasser. Den første foreldreklassen er OnionParent. Den inneholder metoder for sending og mottaking av data, pluss diverse metoder for håndtering av byte-arrays. Alle tre nodeklassene arver fra OnionParent (Selv om noen gjør det indirekte.) Den andre foreldreklassen er OnionEndPoint. Denne inneholder instrukser for hvordan å å velge ut sett moder noder og så kryptere meldingen for alle nodene. Den inneholder i tillegg metoder for å få tak i nøkler fra alle nodene. Denne klassen arver fra OnionParent og er arvet av OnionClient og OnionServer. På den måten arver alle tre nodeklassene fra OnionParent mens bare OnionServer og OnionClient arver fra OnionEndPoint.
3. MainKlasse:  
Mainklassen OnionMain setter opp nodene, og setter så opp klienten og serveren, ellers har den ikke noen annen funksjon.
4. Enumeratorklasse:  
Alle meldingene som blir sendt i produktet starter med en byte som signifiserer hvilke type melding det er. F.eks. nøkkelbytte, serverforespørsel, webforespørsel eller oppdelt svar. MessageNode er en enumerator som gir navn til de forskjellige byte-verdiene. Produktet hadde funket uten denne klassen, men å ha den med gjør at programmene blir lettere å skrive og lese da man ikke trenger å huske hvilken byte-verdi som var server forespørsel og hvilken som var web-forespørsel.  
  
### Kommentarer til diverse deler av produktet:    
### Rom for utvidelse:  
Selv om produktet bare har en server, en klient og alle noder er på samme IP, har jeg prøvd å programmere slik at man kan legge til flere klienter, servere og noder (bl.a. på andre maskiner) uten noen store endringer i koden. (Se fremtidig arbeid).  
  
### Kommunikasjon:  
Nodene, klienten og serveren kommuniserer med hverandre ved hjelp av sokketprogrammering og datagram. På den ene siden er dette en lettvint løsning som er lett og implementere. I produktet slik det er skrevet idag er alle nodene på samme IP, men programmet er implementert slik at hvis man hadde lagt til IP-en til en annen person som kjører programmet, så burde det gå ann å kommunisere med hans/hennes noder og server. Nedsiden med sokketprogrammering er at de kan få problemer med for lange meldinger. (Se muligheter for forbedring).
Meldinger starter med et flagg som sier hvilke type melding det er. Flagget består av en byte, og vi bruker enumeratoren MessageMode for å gjøre det mer leselig. De forskjellige flaggene er:  
KEY_EXCHANGE: En forespørsel om eller svar med en nodes offentlige nøkkel  
FORWARD_ON_NETWORK: Sier til serveren at den skal brukes som echo server. Er også default flagg hvis ikke andre flagg er relevante. Alle meldinger til noder er FORWARD_ON_NETWORK utenom KEY_EXCHANGE meldinger.
FORWARD_TO_WEB: Sier til serveren at den skal sende forespørselen ut på nettet.  
SPLIT_RESPONSE: Brukes på forespørseler som er for lange for en melding.  
  
Her er en liste med hvordan de forskjellige typebe ned forespørsler ser ut. Krypterte deler av meldingen er inni paranteser:  
En KEY_EXCHANGE forespørsel har følgende form:  
Flagg  
Hele forespørselen er på en byte, altså flagget. Responsen har følgende form:  
Flagg + Public-Key
Her sendes bare flagget og den offentlige nøkkelen.  
En standard FORWARD_ON_NETWORK forespørsel har følgende form:  
Flagg + (AESnøkkel) + (Adresse + Payload)  
Flagget gir meldingstypen, Den første parantesen er kryptert med RSA og er AES nøkkelen som man bruker for å dekryptere den andre parantesen. Addressen gir addressen til neste node man skal sende til.  
En FORWARD_ON_NETWORK eller FORWARD_TO_WEB til en server eller klient har følgende form:
Flagg + Addresse + payload
Flagget viser hvilke type forespørsel det er. Addressen er returAddressen responsen skal sendes til (Merk, den sendes ikke direkte, men går første gjennom flere lag med noder.) Payloaden er forespørslen, altså enten det som skal echoes eller url som skal besøkes.  
En SPLIT_RESPONSE forespørsel har følgende form:  
Flagg + delnummer + antdeler + payload  
Delnummeret brukes til å si hvilket nummer av delene som meldingen er og brukes for å sette sammen meldingen i tilfelle ikke alle meldingene kom fram i samme rekkefølge som de ble sendt. Antdeler brukes til å si hvor mange deler meldingen er splittet opp i. 



### Kryptering: 

Når det gjelder valg av krypteringsmetoder hadde jeg en tre faktorer som jeg tenkte på:    
1. Hver node-klient forhold må kunne kryptere uten at andre kan lese det. Det vil si at flere noder med samme klient må ikke kunne dekryptere hverandres meldinger. På samme måte må flere klienter med samme node ikke kunne dekryptere hverandres meldinger.
2. Hvor mulig skal klienter og servere ha ansvar for å velge og huske hvilke noder de vil bruke for å sende meldinger. Slik programmet er nå velger klienten og serveren alle nodene når programmet starter, men nodene er implementert slik at midt i programmet kan en annen klient begynne å bruke nodene som er satt opp på en annen PC. Klienten bør selv kunne velge noder den vil bruke for å sende gjennom. I tillegg bør så mye som mulig av ansvaret til å lagre og bruke en delt hemmelighet ligge hos klienten. Jeg vil ikke ha et scenario hvor en node som blir brukt av mange ulike klienter husker for mange delte hemmeligheter, og er usikker på om noen av klientene er ferdige så dataen til klienten kan slettes.
3. Ettersom symmetrisk kryptering er fortere enn usymmetrisk, bør mest mulig av kryptering og dekryptering gjør symmetrisk.
Med disse tre faktorene, landet jeg på følgende løsning: Hver node har sitt eget par med public og private nøkkel (RSA). Når en klient (eller server) har valgt ut hvilke noder den vil bruke i starten av programmet, sender den ut en forespørsel til noder om å få nodens offentlige nøkkel. Når en melding skal krypteres, lager man først en symmetrisk AES nøkkel for hver node som man krypter meldingen med. Så krypterer man AES nøkkelen med nodens offentlige nøkkel. Når noden får meldingen bruker den sin private nøkkel på å dekryptere AES nøkkelen. Den bruker så den AES nøkkelen til å dekryptere resten av meldingen.


### Routing
Hver gang en melding sendes fra klient til server eller motsatt, blir det valgt ut 3 tilfeldige noder ut av 10. (Disse tallene kan også endres lett.) Dette vil anonymisere hvor meldinger kommer fra og til ettersom det ikke vil være noe mønster om hvor en node sender medlingen. Svaret vil også ta en annen rute en den det var sendt langs. I tillegg, hvis meldingen splittes opp, vil hver individuelle meldingdel ta sin egen vei gjennom nodene, som gjør det nesten umulig å se hvilken hvor meldingen kommer fra og skal til.



## Avhengigheter
Når det gjelder pakker som jeg importerer bruker jeg java.crypto og java.security for krypteringen. java.net bruker jeg for å sende forespørseler på nettet og java.util gir meg diverse andre funksjonaliteter som Arraylist.

## Instruksjoner
For å bruke programme kjører man javafilen OnionMain. Da vil klient, server og node settes opp, og klienten og serveren vil få tak i nøklene til nodene. Etter det er ferdig sendes en testmelding til serveren og serveren svarer med samme melding og man får opp "Success!". Så kommer man inn i loopen til programmet.
Man har her tre alternativer:  
1: Echo server  
2: Web server    
3: Terminate  
Hvis man trykker 1 vil man bruke echo serveren. Da vil man få sjansen til å skrive inn en linje som blir sendt til serveren. Serveren vil så herme og sende tilbake samme melding.  
Hvis man trykker 2 vil man bruke web serveren. Da vil man kunne skrive inn en url, som blir sendt til serveren. Serveren kjører da forespørselen og man vil få tilbake svaret på en lang tekst streng til klienten. Merk at hvis responsen er på over 256 000 bytes, vil man få en error. (Se fremtidig arbeid for årsak og mulige løsninger). url-en https://www.google.com er et eksempel på en url som fungerer.  
Hvis man trykker 3 (eller noe annet enn 1 eller 2) vil klienten avsluttes.  

## Fremtidig arbeid
### Mangel på kryptering mellom klient og server
Slik programmet er skrevet nå, er det ikke noe kryptering mellom klient og server. Det vil si at den siste noden før serveren kan i praksis lese hele meldingen. Dette er selvfølgelig ikke optimalt i program hvor hele poenget er at det skal være anonymt og hemmelig. Dette kan bli fikset relativt lett med å gi både klient og server sitt eget RSA nøkkelpar, og å la dem key-exchange med hverandre i starten av programmet. Å endre på dette er det første jeg hadde endret på hvis jeg hadde hatt mer tid.
### Problemer med for store meldinger   
I og med at jeg bruker Datagram, takler den ikke altfor store meldinger. Derfor blir store responser fra werbrequest splittet opp i flere mindre deler. Måten det er implementert på nå er det en byte som representerer hvor mange deler requesten er splittet opp i. Det er da tydelig at hvis responsen er for stor, og meldingen blir splittet opp i flere enn 255 deler, så vil dette gå galt. Å fikse på dette er den andre tingen jeg hadde gjort hvis jeg skulle endre på noe. Dette kunne enten bli gjort ved å ha mer plass til å anngi anntall deler, eller ved å bruke en annen overføringmetode enn Datagram.  

### Stoppe alle noder + server når klient avslutter
Ettersom programmet for øyeblikket ikke kommuniserer med eller blir kommunisert med fra andre maskiner, burde programmet egentlig avslutte nodene og serveren når klienten avslutter. Dette er ikke et stort problem, men det hadde vært greit å få fikset. 

### La key exchange først sende ut mange requester og så ta imot svar.
Slik det er nå vil serveren og klienten sende ut en request og så vente på svar fra requesten før den sender ut neste. Jeg mener det ville være mer effektivt at klienten og serveren først sender ut alle requestene sine, og så venter på svarene.

### Legge opp for flere klienter og servere
Slik programmet er nå kjører alle nodene og klienten+server på samme maskin. Men slik jeg har programmert det bør det ikke være vanskelig å tillate den med å bruke noder på andre maskiner. En måte å gjøre dette på er la brukeren skrive inn hvilke Ip-addresse den vil lete etter noder på, og å la brukeren skrive inn hvilke IP addresse + port den skal bruke når den sender melding til Server. En mer avansert måte hadde hatt et sett med noder som er permanent tilgjenelig på internettet som man kan bruke.

### Utvide serveren
Utenom å være en webserver, så kan serveren bli programmert til å gjøre flere andre ting, som å inneholde en database eller kjøre et annet verktøy som man vil kunne kommunisere med anonymt.

### Sette opp egen router.
Man kunne i teorien la klienten også være en server på localhost (eller legge det ut på internettet). Serveren vil da ta en Url som et JSON object, kjøre det anonymt, og så sende html responsen til nettleseren til brukeren, noe som på den måten lar brukeren bruke internettet anonymt. Dette er nok det vanskeligste ut av ideene til fremtidig arbeid og vil kreve en god del resurser, men det virker mulig.

## Lenke til Repo
Lenke til Github repo: https://github.com/biasedLiar/OnionRouting2  
I repoen kan man ikke se de tidligste committene.   
Det er fordi jeg lagde originalt prosjekt inni en annen repo. Problemet var at jeg med et uhell valgte feil mappe some root, så resultatet ble at det var to forskjellige prosjekter i samme repo. Når jeg oppdaget problemet flyttet jeg filene til en ny mappe og lagde en ny repo. Hvis man er interressert i å se på selve utviklingen av prosjektet, kan man finne det i repoen: https://github.com/biasedLiar/OnionRouting  
Merk at root katalogen til prosjektet er under mappen Onionproject, og at de andre filene og mappene ikke tilhører dette prosjektet.  
