kadaster-gds2
=============

JAX-WS classes voor benaderen van de Generieke Download Service 2 van het Kadaster. 

## Schemas

De bestanden in de schemas/ directory zijn afkomstig van het Kadaster.

## Maken keystore met PKIoverheid certificaat

Je hebt een PKIoverheid certificaat nodig waarvan het publieke deel bij Kadaster bekend is. Hiermee word je geauthenticeerd en aan de juiste afgiftes gekoppeld aan de serverkant bij Kadaster.

Dit certificaat moet worden geimporteerd in een keystore (src/main/resources/gds2_key.jks). Uitgaande van PEM encoded certificaat en key, doe als volgt:

Maken PKCS12 bestand met certificaat en key. Verifieert ook dat key en certificaat overeenkomen.
`openssl pkcs12 -export -out mycert.p12 -inkey mykey.key -in mycert.crt -name mycert`

Importeren in keystore:
`keytool -importkeystore -destkeystore src/main/resources/gds2_key.jks -srckeystore mycert.p12 -srcstoretype pkcs12 -alias mycert`

Gebruik als wachtwoord "changeit".


## GDS2 listing

Maak een build en `java -cp ./kadaster-gds2-1.2-SNAPSHOT.jar nl.b3p.gds2.Main > gds2.log` zorg dat naast de jar file een bestand `private.key` en een bestand `public.key` met de juiste inhoud staan.
Als alternatief kun je een keystore file opgeven `gds2_key.jks` en any commandline argument `java -cp ./kadaster-gds2-1.2-SNAPSHOT.jar nl.b3p.gds2.Main any > gds2.log`
